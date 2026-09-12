package dev.pinaki.localnotes.server

import java.io.BufferedInputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking

/** Serves registered REST and HTML CRUD controllers. */
class Server(port: Int = DEFAULT_PORT) : AutoCloseable {
    private val serverSocket = ServerSocket().apply {
        reuseAddress = true
        bind(InetSocketAddress(BIND_ADDRESS, port))
    }
    private val executor = Executors.newCachedThreadPool()
    private val running = AtomicBoolean(false)
    private val controllers = CopyOnWriteArrayList<RestController>()
    private val htmlControllers = CopyOnWriteArrayList<HtmlCrudController>()

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
            val route = findController(requestPath)
            if (route != null) {
                val body = input.readNBytes(
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

        @JvmStatic
        fun main(args: Array<String>) {
            val server = Server(args.firstOrNull()?.toInt() ?: DEFAULT_PORT)
            Runtime.getRuntime().addShutdownHook(Thread(server::close))
            server.start()
            server.getNetworkAddresses().forEach { println("Serving at $it") }
        }

    }
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

private fun String.normalizedControllerPath(): String {
    require(isNotBlank()) { "Controller path must not be blank" }
    val normalized = "/${trim().trim('/')}"
    require(normalized != "/") { "Controller path must not be the root path" }
    return normalized
}
