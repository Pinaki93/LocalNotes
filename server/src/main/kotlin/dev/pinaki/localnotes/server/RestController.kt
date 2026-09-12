package dev.pinaki.localnotes.server

interface RestController {
    val path: String

    suspend fun get(): RestResponse = RestResponse.methodNotAllowed()
    suspend fun get(id: String): RestResponse = RestResponse.methodNotAllowed()
    suspend fun post(body: String): RestResponse = RestResponse.methodNotAllowed()
    suspend fun put(id: String, body: String): RestResponse = RestResponse.methodNotAllowed()
    suspend fun delete(id: String): RestResponse = RestResponse.methodNotAllowed()
}

data class RestResponse(
    val statusCode: Int,
    val reason: String,
    val body: String = "",
    val contentType: String = "application/json; charset=utf-8",
    val headers: Map<String, String> = emptyMap(),
) {
    companion object {
        fun ok(body: String): RestResponse = RestResponse(200, "OK", body)
        fun notFound(body: String = "Not found") =
            RestResponse(404, "Not Found", body, "text/plain; charset=utf-8")
        fun methodNotAllowed() =
            RestResponse(405, "Method Not Allowed", "Method not allowed", "text/plain; charset=utf-8")
    }
}
