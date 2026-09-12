package dev.pinaki.localnotes.server

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RestControllerTest {
    private lateinit var server: Server
    private val client = HttpClient.newHttpClient()

    @Before
    fun setUp() {
        server = Server(0).registerController(TestController())
        server.start()
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `routes standard REST methods`() {
        assertResponse("GET", "/api/items", expectedBody = "all")
        assertResponse("GET", "/api/items/note-1", expectedBody = "get:note-1")
        assertResponse("POST", "/api/items", "café", "post:café")
        assertResponse("PUT", "/api/items/note-1", "updated", "put:note-1:updated")
        assertResponse("DELETE", "/api/items/note-1", expectedBody = "delete:note-1")
    }

    @Test
    fun `rejects controller registration after start`() {
        val error = runCatching { server.registerController(TestController()) }.exceptionOrNull()
        assertEquals("Controllers must be registered before the server starts", error?.message)
    }

    private fun assertResponse(method: String, path: String, body: String = "", expectedBody: String) {
        val request = HttpRequest.newBuilder(URI.create("http://localhost:${server.getPort()}$path"))
            .method(method, if (body.isEmpty()) HttpRequest.BodyPublishers.noBody() else HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, response.statusCode())
        assertEquals(expectedBody, response.body())
    }

    private class TestController : RestController {
        override val path = "/api/items/"

        override suspend fun get() = RestResponse.ok("all")
        override suspend fun get(id: String) = RestResponse.ok("get:$id")
        override suspend fun post(body: String) = RestResponse.ok("post:$body")
        override suspend fun put(id: String, body: String) = RestResponse.ok("put:$id:$body")
        override suspend fun delete(id: String) = RestResponse.ok("delete:$id")
    }
}
