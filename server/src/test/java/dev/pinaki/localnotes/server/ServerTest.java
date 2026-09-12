package dev.pinaki.localnotes.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServerTest {
    private Server server;
    private HttpClient client;

    @Before
    public void setUp() throws Exception {
        server = new Server(0);
        server.start();
        client = HttpClient.newHttpClient();
    }

    @After
    public void tearDown() {
        server.close();
    }

    @Test
    public void rootServesIndexHtml() throws Exception {
        HttpResponse<String> response = sendGet("/");

        assertEquals(200, response.statusCode());
        assertEquals("text/html; charset=utf-8", response.headers().firstValue("Content-Type").orElse(""));
        assertTrue(response.body().contains("LocalNotes"));
    }

    @Test
    public void unknownFileReturnsNotFound() throws Exception {
        HttpResponse<String> response = sendGet("/missing.html");

        assertEquals(404, response.statusCode());
        assertEquals("Not found", response.body());
    }

    @Test
    public void unregisteredApiReturnsNotFound() throws Exception {
        HttpResponse<String> response = sendGet("/api/notes");

        assertEquals(404, response.statusCode());
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + server.getPort() + path)
        ).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
