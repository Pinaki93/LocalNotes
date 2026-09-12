package dev.pinaki.localnotes.server

import java.io.BufferedInputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URLDecoder
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking

/** Serves registered REST and HTML CRUD controllers. */
class Server @JvmOverloads constructor(
    port: Int = DEFAULT_PORT,
    private val passwordAuthenticator: PasswordAuthenticator? = null,
) : AutoCloseable {
    private val serverSocket = ServerSocket().apply {
        reuseAddress = true
        bind(InetSocketAddress(BIND_ADDRESS, port))
    }
    private val executor = Executors.newCachedThreadPool()
    private val running = AtomicBoolean(false)
    private val controllers = CopyOnWriteArrayList<RestController>()
    private val htmlControllers = CopyOnWriteArrayList<HtmlCrudController>()
    private val sessions = ConcurrentHashMap<String, Long>()
    private val secureRandom = SecureRandom()

    fun registerController(controller: RestController): Server = apply {
        require(!running.get()) { "Controllers must be registered before the server starts" }
        val path = controller.path.normalizedControllerPath()
        require(controllers.none { it.path.normalizedControllerPath() == path }) {
            "A controller is already registered for $path"
        }
        controllers += controller
    }

    fun registerController(controller: HtmlCrudController): Server = apply {
        require(!running.get()) { "Controllers must be registered before the server starts" }
        val path = controller.path.normalizedControllerPath()
        require(htmlControllers.none { it.path.normalizedControllerPath() == path }) {
            "An HTML controller is already registered for $path"
        }
        require(!controller.servesRoot || htmlControllers.none(HtmlCrudController::servesRoot)) {
            "An HTML controller is already registered for the root path"
        }
        htmlControllers += controller
    }

    fun start() {
        if (!running.compareAndSet(false, true)) return
        executor.execute {
            while (running.get()) {
                try {
                    val socket = serverSocket.accept()
                    executor.execute { handleSafely(socket) }
                } catch (error: Exception) {
                    if (running.get()) error.printStackTrace()
                }
            }
        }
    }

    fun getPort(): Int = serverSocket.localPort

    fun getNetworkAddresses(): List<String> {
        val addresses = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .asSequence().filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<Inet4Address>().filter { !it.isLoopbackAddress }
            .map { "http://${it.hostAddress}:${getPort()}" }.toList()
        return addresses.ifEmpty { listOf("http://localhost:${getPort()}") }
    }

    override fun close() {
        if (!running.getAndSet(false)) return
        serverSocket.close()
        executor.shutdownNow()
    }

    private fun handle(socket: Socket) {
        socket.use { connection ->
            connection.soTimeout = SOCKET_TIMEOUT_MILLIS
            val input = BufferedInputStream(connection.getInputStream())
            val requestParts = input.readHttpLine()?.split(' ') ?: return
            val headers = readHeaders(input)
            if (requestParts.size < 2) {
                connection.sendResponse(
                    RestResponse(
                        400,
                        "Bad Request",
                        "Bad request",
                        "text/plain; charset=utf-8"
                    )
                )
                return
            }

            val requestPath = runCatching { URI(requestParts[1]).path }.getOrNull()
            if (requestPath == null) {
                connection.sendResponse(RestResponse.notFound())
                return
            }
            if (passwordAuthenticator != null) {
                if (requestPath == LOGIN_PATH) {
                    handleLogin(connection, requestParts[0], headers, input)
                    return
                }
                if (!isAuthenticated(headers)) {
                    connection.sendResponse(
                        if (requestPath.startsWith("/api/")) RestResponse(
                            401,
                            "Unauthorized",
                            "Authentication required",
                            "text/plain; charset=utf-8",
                        ) else RestResponse(
                            303,
                            "See Other",
                            headers = mapOf("Location" to LOGIN_PATH),
                        ),
                    )
                    return
                }
            }
            val route = findController(requestPath)
            if (route != null) {
                val body = input.readUpTo(
                    (headers["content-length"]?.toIntOrNull() ?: 0).coerceAtLeast(0)
                )
                    .toString(Charsets.UTF_8)
                connection.sendResponse(runBlocking { route.dispatch(requestParts[0], body) })
                return
            }

            if (requestParts[0] != "GET") {
                connection.sendResponse(
                    RestResponse.methodNotAllowed().copy(headers = mapOf("Allow" to "GET"))
                )
                return
            }
            val htmlRoute = findHtmlController(requestPath)
            if (htmlRoute == null) {
                connection.sendResponse(RestResponse.notFound())
                return
            }
            val pageResource = runBlocking { htmlRoute.resourcePath() }
                ?.takeIf(String::isValidHtmlResourcePath)
            if (pageResource == null) {
                connection.sendResponse(RestResponse.notFound())
                return
            }
            Server::class.java.getResourceAsStream(pageResource).use { input ->
                if (input == null) {
                    connection.sendResponse(RestResponse.notFound())
                    return
                }
                connection.send(200, "OK", "text/html; charset=utf-8", input.readBytes())
            }
        }
    }

    private fun handleLogin(
        connection: Socket,
        method: String,
        headers: Map<String, String>,
        input: BufferedInputStream,
    ) {
        when (method) {
            "GET" -> connection.sendResponse(loginPage())
            "POST" -> {
                val body = input.readUpTo(
                    (headers["content-length"]?.toIntOrNull() ?: 0).coerceIn(0, MAX_LOGIN_BODY_BYTES),
                ).toString(Charsets.UTF_8)
                val password = body.split('&').firstNotNullOfOrNull { field ->
                    val parts = field.split('=', limit = 2)
                    if (parts.firstOrNull() == "password") {
                        URLDecoder.decode(parts.getOrElse(1) { "" }, Charsets.UTF_8.name())
                    } else null
                }.orEmpty()
                if (passwordAuthenticator?.authenticate(password) == true) {
                    val tokenBytes = ByteArray(32).also(secureRandom::nextBytes)
                    val token = tokenBytes.joinToString("") { "%02x".format(it) }
                    sessions[token] = System.currentTimeMillis() + SESSION_LIFETIME_MILLIS
                    connection.sendResponse(
                        RestResponse(
                            303,
                            "See Other",
                            headers = mapOf(
                                "Location" to "/",
                                "Set-Cookie" to "$SESSION_COOKIE=$token; Path=/; HttpOnly; SameSite=Strict",
                            ),
                        ),
                    )
                } else {
                    connection.sendResponse(loginPage(invalid = true))
                }
            }
            else -> connection.sendResponse(
                RestResponse.methodNotAllowed().copy(headers = mapOf("Allow" to "GET, POST")),
            )
        }
    }

    private fun isAuthenticated(headers: Map<String, String>): Boolean {
        val token = headers["cookie"]?.split(';')?.firstNotNullOfOrNull { cookie ->
            val parts = cookie.trim().split('=', limit = 2)
            parts.getOrNull(1)?.takeIf { parts[0] == SESSION_COOKIE }
        } ?: return false
        val expiresAt = sessions[token] ?: return false
        if (expiresAt <= System.currentTimeMillis()) {
            sessions.remove(token)
            return false
        }
        return true
    }

    private fun loginPage(invalid: Boolean = false) = RestResponse(
        if (invalid) 401 else 200,
        if (invalid) "Unauthorized" else "OK",
        """<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="theme-color" content="#ffe34f">
    <title>Sign in · LocalNotes</title>
    <style>
        :root { font-family:Inter,ui-sans-serif,system-ui,-apple-system,sans-serif; color:#171717; background:#fffaf0 }
        * { box-sizing:border-box }
        body { min-height:100vh; margin:0; display:grid; grid-template-rows:auto 1fr; background-image:radial-gradient(#171717 1px,transparent 1px); background-size:22px 22px }
        .topbar { display:flex; align-items:center; justify-content:space-between; gap:1rem; padding:1.15rem clamp(1rem,5vw,3rem); background:#ffe34f; border-bottom:3px solid #171717 }
        .brand { display:flex; align-items:center; gap:.75rem; font-family:Georgia,serif; font-size:clamp(1.25rem,4vw,1.6rem); font-weight:900; letter-spacing:-.03em }
        .logo { display:grid; place-items:center; width:2.35rem; height:2.35rem; background:#fff; border:2px solid #171717; box-shadow:3px 3px #171717 }
        .local { padding:.38rem .65rem; font-size:.72rem; font-weight:900; letter-spacing:.1em; text-transform:uppercase; background:#7ce7a7; border:2px solid #171717 }
        main { width:min(100% - 2rem,29rem); margin:auto; padding:3rem 0 }
        .eyebrow { display:inline-block; margin:0 0 .85rem; padding:.35rem .65rem; background:#7ce7a7; border:2px solid #171717; font-size:.75rem; font-weight:900; letter-spacing:.12em }
        .card { position:relative; padding:clamp(1.35rem,5vw,2rem); background:#79cef2; border:3px solid #171717; box-shadow:8px 8px #171717 }
        h1 { margin:0; font-family:Georgia,serif; font-size:clamp(2rem,9vw,3rem); line-height:.95; letter-spacing:-.045em }
        .intro { margin:.9rem 0 1.65rem; line-height:1.55; font-weight:600 }
        label { display:grid; gap:.55rem; font-family:Georgia,serif; font-size:.88rem; font-weight:900; letter-spacing:.06em; text-transform:uppercase }
        input { width:100%; min-height:3.4rem; padding:.8rem 1rem; color:#171717; background:#fff; border:3px solid #171717; border-radius:0; outline:none; font:inherit; font-size:1rem; font-weight:600; box-shadow:4px 4px #171717; transition:transform .12s,box-shadow .12s }
        input:focus { transform:translate(2px,2px); box-shadow:2px 2px #171717 }
        button { width:100%; min-height:3.5rem; margin-top:1.35rem; padding:.8rem 1rem; color:#171717; background:#ffe34f; border:3px solid #171717; border-radius:0; box-shadow:5px 5px #171717; font:900 1rem Georgia,serif; letter-spacing:.04em; text-transform:uppercase; cursor:pointer; transition:transform .12s,box-shadow .12s }
        button:hover { background:#ffed86 }
        button:active { transform:translate(4px,4px); box-shadow:1px 1px #171717 }
        .error { margin:0 0 1.15rem; padding:.75rem .85rem; background:#ff8f88; border:2px solid #171717; font-weight:800 }
        .hint { display:flex; align-items:flex-start; gap:.6rem; margin:1.35rem 0 0; font-size:.8rem; font-weight:650; line-height:1.4 }
        .dot { flex:0 0 auto; width:.65rem; height:.65rem; margin-top:.2rem; background:#7ce7a7; border:2px solid #171717; border-radius:50% }
        @media(max-width:360px) { .local { display:none } .card { box-shadow:6px 6px #171717 } }
        @media(prefers-reduced-motion:reduce) { input,button { transition:none } }
    </style>
</head>
<body>
    <header class="topbar">
        <div class="brand"><span class="logo">N</span><span>LocalNotes</span></div>
        <span class="local">Local network</span>
    </header>
    <main>
        <section class="card" aria-labelledby="login-title">
            <p class="eyebrow">PRIVATE ACCESS</p>
            <h1 id="login-title">Welcome back.</h1>
            <p class="intro">Enter the password set in the LocalNotes app to open your notes.</p>
            ${if (invalid) "<p class=\"error\" role=\"alert\">That password is incorrect. Try again.</p>" else ""}
            <form method="post" action="/login">
                <label for="password">Web password</label>
                <input id="password" name="password" type="password" required autofocus autocomplete="current-password">
                <button type="submit">Open my notes →</button>
            </form>
            <p class="hint"><span class="dot" aria-hidden="true"></span><span>Only a one-way password verifier is saved on Android. Connect only from a network you trust.</span></p>
        </section>
    </main>
</body>
</html>""",
        "text/html; charset=utf-8",
        mapOf("Cache-Control" to "no-store"),
    )

    private fun readHeaders(input: BufferedInputStream): Map<String, String> = buildMap {
        while (true) {
            val header = input.readHttpLine() ?: break
            if (header.isEmpty()) break
            val separator = header.indexOf(':')
            if (separator > 0) put(
                header.substring(0, separator).trim().lowercase(Locale.ROOT),
                header.substring(separator + 1).trim(),
            )
        }
    }

    private fun findController(requestPath: String): ControllerRoute? =
        controllers.firstNotNullOfOrNull { controller ->
            val path = controller.path.normalizedControllerPath()
            when {
                requestPath == path -> ControllerRoute(controller, null)
                requestPath.startsWith("$path/") -> requestPath.removePrefix("$path/")
                    .takeIf { it.isNotEmpty() && '/' !in it }
                    ?.let { ControllerRoute(controller, it) }

                else -> null
            }
        }

    private fun findHtmlController(requestPath: String): HtmlControllerRoute? =
        htmlControllers.firstNotNullOfOrNull { controller ->
            val path = controller.path.normalizedControllerPath()
            when {
                requestPath == "/" && controller.servesRoot ->
                    HtmlControllerRoute(controller, HtmlOperation.LISTING)
                requestPath == path -> HtmlControllerRoute(controller, HtmlOperation.LISTING)
                requestPath == "$path/create" -> HtmlControllerRoute(controller, HtmlOperation.CREATE)
                requestPath == "$path/update" -> HtmlControllerRoute(controller, HtmlOperation.UPDATE)
                requestPath == "$path/delete" -> HtmlControllerRoute(controller, HtmlOperation.DELETE)
                else -> null
            }
        }

    private suspend fun ControllerRoute.dispatch(method: String, body: String): RestResponse =
        when (method) {
            "GET" -> id?.let { controller.get(it) } ?: controller.get()
            "POST" -> if (id == null) controller.post(body) else RestResponse.methodNotAllowed()
            "PUT" -> id?.let { controller.put(it, body) } ?: RestResponse.methodNotAllowed()
            "DELETE" -> id?.let { controller.delete(it) } ?: RestResponse.methodNotAllowed()
            else -> RestResponse.methodNotAllowed()
        }

    private fun handleSafely(socket: Socket) {
        try {
            handle(socket)
        } catch (_: SocketTimeoutException) {
            runCatching { socket.close() }
        } catch (error: Exception) {
            runCatching { socket.close() }
            if (running.get()) error.printStackTrace()
        }
    }

    private fun Socket.sendResponse(response: RestResponse) = send(
        response.statusCode, response.reason, response.contentType,
        response.body.toByteArray(Charsets.UTF_8), response.headers,
    )

    private fun Socket.send(
        statusCode: Int, reason: String, contentType: String, body: ByteArray,
        extraHeaders: Map<String, String> = emptyMap(),
    ) {
        getOutputStream().buffered().use { output ->
            val headers = buildString {
                append("HTTP/1.1 $statusCode $reason\r\n")
                append("Content-Type: $contentType\r\n")
                append("Content-Length: ${body.size}\r\n")
                append("Connection: close\r\n")
                extraHeaders.forEach { (name, value) -> append("$name: $value\r\n") }
                append("\r\n")
            }
            output.write(headers.toByteArray(Charsets.US_ASCII))
            output.write(body)
        }
    }

    companion object {
        const val DEFAULT_PORT = 8080
        private const val BIND_ADDRESS = "0.0.0.0"
        private const val SOCKET_TIMEOUT_MILLIS = 5_000
        private const val LOGIN_PATH = "/login"
        private const val SESSION_COOKIE = "localnotes_session"
        private const val SESSION_LIFETIME_MILLIS = 12 * 60 * 60 * 1_000L
        private const val MAX_LOGIN_BODY_BYTES = 8 * 1024

        @JvmStatic
        fun main(args: Array<String>) {
            val server = Server(args.firstOrNull()?.toInt() ?: DEFAULT_PORT)
            Runtime.getRuntime().addShutdownHook(Thread(server::close))
            server.start()
            server.getNetworkAddresses().forEach { println("Serving at $it") }
        }

    }
}

fun interface PasswordAuthenticator {
    fun authenticate(password: String): Boolean
}

private data class ControllerRoute(val controller: RestController, val id: String?)

private data class HtmlControllerRoute(
    val controller: HtmlCrudController,
    val operation: HtmlOperation,
) {
    suspend fun resourcePath(): String? = when (operation) {
        HtmlOperation.LISTING -> controller.listing()
        HtmlOperation.CREATE -> controller.create()
        HtmlOperation.UPDATE -> controller.update()
        HtmlOperation.DELETE -> controller.delete()
    }
}

private enum class HtmlOperation { LISTING, CREATE, UPDATE, DELETE }

private fun String.isValidHtmlResourcePath(): Boolean =
    startsWith('/') && endsWith(".html", ignoreCase = true) &&
        split('/').none { it == ".." || it == "." }

private fun BufferedInputStream.readHttpLine(): String? {
    val bytes = mutableListOf<Byte>()
    while (true) {
        val value = read()
        if (value < 0) return bytes.takeIf { it.isNotEmpty() }?.toByteArray()
            ?.toString(Charsets.US_ASCII)
        if (value == '\n'.code) {
            if (bytes.lastOrNull() == '\r'.code.toByte()) bytes.removeAt(bytes.lastIndex)
            return bytes.toByteArray().toString(Charsets.US_ASCII)
        }
        bytes += value.toByte()
    }
}

private fun BufferedInputStream.readUpTo(byteCount: Int): ByteArray {
    val bytes = ByteArray(byteCount)
    var offset = 0
    while (offset < byteCount) {
        val count = read(bytes, offset, byteCount - offset)
        if (count < 0) break
        offset += count
    }
    return if (offset == byteCount) bytes else bytes.copyOf(offset)
}

private fun String.normalizedControllerPath(): String {
    require(isNotBlank()) { "Controller path must not be blank" }
    val normalized = "/${trim().trim('/')}"
    require(normalized != "/") { "Controller path must not be the root path" }
    return normalized
}
