package dev.pinaki.localnotes.server

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthenticationTest {
    private lateinit var server: Server
    private val client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    @Before
    fun setUp() {
        server = Server(0, PasswordAuthenticator { it == "correct horse" })
            .registerController(TestController())
            .registerController(TestHtmlController())
        server.start()
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `unauthenticated pages redirect and APIs return unauthorized`() {
        val page = send("GET", "/")
        val api = send("GET", "/api/items")

        assertEquals(303, page.statusCode())
        assertEquals("/login", page.headers().firstValue("Location").orElse(""))
        assertEquals(401, api.statusCode())
    }

    @Test
    fun `login creates a session that authenticates pages and APIs`() {
        assertEquals(401, send("POST", "/login", "password=wrong").statusCode())

        val login = send("POST", "/login", "password=correct+horse")
        assertEquals(303, login.statusCode())
        val cookie = login.headers().firstValue("Set-Cookie").orElse("")
        assertTrue(cookie.contains("HttpOnly"))
        assertTrue(cookie.contains("SameSite=Strict"))

        val sessionCookie = cookie.substringBefore(';')
        assertEquals(200, send("GET", "/", cookie = sessionCookie).statusCode())
        assertEquals(200, send("GET", "/api/items", cookie = sessionCookie).statusCode())
    }

    private fun send(
        method: String,
        path: String,
        body: String = "",
        cookie: String? = null,
    ): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(URI.create("http://localhost:${server.getPort()}$path"))
            .method(method, if (body.isEmpty()) HttpRequest.BodyPublishers.noBody() else HttpRequest.BodyPublishers.ofString(body))
        if (body.isNotEmpty()) builder.header("Content-Type", "application/x-www-form-urlencoded")
        if (cookie != null) builder.header("Cookie", cookie)
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private class TestController : RestController {
        override val path = "/api/items"
        override suspend fun get() = RestResponse.ok("items")
    }

    private class TestHtmlController : HtmlCrudController {
        override val path = "/items"
        override val servesRoot = true
        override suspend fun listing() = "/pages/listing.html"
    }
}
