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
        server = new Server(0).registerController(new TestHtmlController());
        server.start();
        client = HttpClient.newHttpClient();
    }

    @After
    public void tearDown() {
        server.close();
    }

    @Test
    public void listingRouteServesIndexHtml() throws Exception {
        HttpResponse<String> response = sendGet("/notes");

        assertEquals(200, response.statusCode());
        assertEquals("text/html; charset=utf-8", response.headers().firstValue("Content-Type").orElse(""));
        assertTrue(response.body().contains("Test listing"));
        assertTrue(response.body().contains("New note"));
        assertTrue(response.body().contains("delete-dialog"));
        assertEquals(404, sendGet("/").statusCode());
    }

    @Test
    public void servesCreateAndUpdatePages() throws Exception {
        HttpResponse<String> createResponse = sendGet("/notes/create");
        HttpResponse<String> updateResponse = sendGet("/notes/update?id=1");

        assertEquals(200, createResponse.statusCode());
        assertTrue(createResponse.body().contains("Create note"));
        assertEquals(200, updateResponse.statusCode());
        assertTrue(updateResponse.body().contains("Edit note"));
        assertEquals(404, sendGet("/notes/delete").statusCode());
        assertEquals(404, sendGet("/create.html").statusCode());
        assertEquals(404, sendGet("/update.html?id=1").statusCode());
    }

    @Test
    public void filesAreNotServed() throws Exception {
        HttpResponse<String> missingResponse = sendGet("/missing.html");
        HttpResponse<String> formerAssetResponse = sendGet("/styles.css");

        assertEquals(404, missingResponse.statusCode());
        assertEquals("Not found", missingResponse.body());
        assertEquals(404, formerAssetResponse.statusCode());
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

    private static class TestHtmlController implements HtmlCrudController {
        @Override public String getPath() { return "/notes"; }
        @Override public Object listing(kotlin.coroutines.Continuation<? super String> continuation) {
            return "/pages/listing.html";
        }
        @Override public Object create(kotlin.coroutines.Continuation<? super String> continuation) {
            return "/pages/create.html";
        }
        @Override public Object update(kotlin.coroutines.Continuation<? super String> continuation) {
            return "/pages/update.html";
        }
        @Override public Object delete(kotlin.coroutines.Continuation<? super String> continuation) {
            return null;
        }
    }
}
