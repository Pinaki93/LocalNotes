package dev.pinaki.localnotes.server

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Serves static files bundled under `src/main/resources/public`. */
class StaticFileServer(port: Int) : AutoCloseable {
    private val serverSocket = ServerSocket().apply {
        reuseAddress = true
        bind(InetSocketAddress(BIND_ADDRESS, port))
    }
    private val executor = Executors.newCachedThreadPool()
    private val running = AtomicBoolean(false)

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
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<Inet4Address>()
            .filter { !it.isLoopbackAddress }
            .map { "http://${it.hostAddress}:${getPort()}" }
            .toList()
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
            val reader = BufferedReader(InputStreamReader(connection.getInputStream(), Charsets.US_ASCII))
            val requestLine = reader.readLine() ?: return
            val requestParts = requestLine.split(' ')
            while (!reader.readLine().isNullOrEmpty()) {
                // Consume request headers.
            }

            if (requestParts.size < 2) {
                connection.send(400, "Bad Request", "text/plain; charset=utf-8", "Bad request".toByteArray())
                return
            }
            if (requestParts[0] != "GET") {
                connection.send(
                    405,
                    "Method Not Allowed",
                    "text/plain; charset=utf-8",
                    "Method not allowed".toByteArray(),
                    mapOf("Allow" to "GET"),
                )
                return
            }

            val resourcePath = resolveResourcePath(requestParts[1])
            if (resourcePath == null) {
                connection.sendNotFound()
                return
            }

            StaticFileServer::class.java.getResourceAsStream(PUBLIC_ROOT + resourcePath).use { input ->
                if (input == null) {
                    connection.sendNotFound()
                    return
                }
                connection.send(200, "OK", contentType(resourcePath), input.readBytes())
            }
        }
    }

    private fun handleSafely(socket: Socket) {
        try {
            handle(socket)
        } catch (_: SocketTimeoutException) {
            // Browsers can leave speculative connections idle. Close them without crashing the app.
            socket.closeQuietly()
        } catch (error: Exception) {
            socket.closeQuietly()
            if (running.get()) error.printStackTrace()
        }
    }

    private fun Socket.closeQuietly() {
        runCatching { close() }
    }

    private fun Socket.sendNotFound() {
        send(404, "Not Found", "text/plain; charset=utf-8", "Not found".toByteArray())
    }

    private fun Socket.send(
        statusCode: Int,
        reason: String,
        contentType: String,
        body: ByteArray,
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
        private const val PUBLIC_ROOT = "/public/"
        private const val SOCKET_TIMEOUT_MILLIS = 5_000

        @JvmStatic
        fun main(args: Array<String>) {
            val port = args.firstOrNull()?.toInt() ?: DEFAULT_PORT
            val staticFileServer = StaticFileServer(port)
            Runtime.getRuntime().addShutdownHook(Thread(staticFileServer::close))
            staticFileServer.start()
            staticFileServer.getNetworkAddresses().forEach { println("Serving HTML at $it") }
        }

        private fun resolveResourcePath(requestTarget: String): String? {
            val path = runCatching { URI(requestTarget).path }.getOrNull() ?: return null
            val parts = mutableListOf<String>()
            for (part in path.split('/')) {
                when (part) {
                    "", "." -> Unit
                    ".." -> return null
                    else -> parts += part
                }
            }
            if (path.endsWith('/') || parts.isEmpty()) parts += "index.html"
            return parts.joinToString("/")
        }

        private fun contentType(path: String): String {
            val lowerPath = path.lowercase(Locale.ROOT)
            return when {
                lowerPath.endsWith(".html") || lowerPath.endsWith(".htm") -> "text/html; charset=utf-8"
                lowerPath.endsWith(".css") -> "text/css; charset=utf-8"
                lowerPath.endsWith(".js") -> "text/javascript; charset=utf-8"
                lowerPath.endsWith(".json") -> "application/json; charset=utf-8"
                lowerPath.endsWith(".svg") -> "image/svg+xml"
                lowerPath.endsWith(".png") -> "image/png"
                lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") -> "image/jpeg"
                else -> "application/octet-stream"
            }
        }
    }
}
