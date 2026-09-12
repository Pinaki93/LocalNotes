package dev.pinaki.localnotes.server

import dev.pinaki.localnotes.data.Note
import dev.pinaki.localnotes.data.NotesRepository
import dev.pinaki.localnotes.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NotesController(
    appContainer: AppContainer,
    override val path: String = "/api/notes",
) : RestController {

    private val repository = appContainer.notesRepository()
    private val jsonFactory = appContainer.json
    private val json get() = jsonFactory.value

    override suspend fun get(): RestResponse = RestResponse.ok(
        json.encodeToString(repository.observeAllNotes().first().map(Note::toResponse)),
    )

    override suspend fun get(id: String): RestResponse {
        val note = repository.getNote(id.toNoteId() ?: return RestResponse.notFound())
            ?: return RestResponse.notFound()
        return RestResponse.ok(json.encodeToString(note.toResponse()))
    }

    override suspend fun post(body: String): RestResponse {
        val request = body.decodeRequest() ?: return badRequest()
        val note = repository.createNote(request.title, request.content)
        return RestResponse(
            statusCode = 201,
            reason = "Created",
            body = json.encodeToString(note.toResponse()),
            headers = mapOf("Location" to "$path/${note.id}"),
        )
    }

    override suspend fun put(id: String, body: String): RestResponse {
        val noteId = id.toNoteId() ?: return RestResponse.notFound()
        val request = body.decodeRequest() ?: return badRequest()
        val existing = repository.getNote(noteId) ?: return RestResponse.notFound()
        if (!repository.updateNote(
                existing.copy(
                    title = request.title,
                    content = request.content
                )
            )
        ) {
            return RestResponse.notFound()
        }
        val updated = repository.getNote(noteId) ?: return RestResponse.notFound()
        return RestResponse.ok(json.encodeToString(updated.toResponse()))
    }

    override suspend fun delete(id: String): RestResponse {
        val noteId = id.toNoteId() ?: return RestResponse.notFound()
        if (!repository.deleteNote(noteId)) return RestResponse.notFound()
        return RestResponse(statusCode = 204, reason = "No Content")
    }

    private fun String.toNoteId(): Int? = toIntOrNull()?.takeIf { it > 0 }

    private fun String.decodeRequest(): NoteRequest? = try {
        json.decodeFromString<NoteRequest>(this)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun badRequest() = RestResponse(
        statusCode = 400,
        reason = "Bad Request",
        body = "Invalid note JSON",
        contentType = "text/plain; charset=utf-8",
    )
}

@Serializable
private data class NoteRequest(val title: String, val content: String)

@Serializable
private data class NoteResponse(
    val id: Int,
    val title: String,
    val content: String,
    val dateModified: Long,
)

private fun Note.toResponse() = NoteResponse(
    id = id,
    title = title,
    content = content,
    dateModified = dateModified.time,
)
