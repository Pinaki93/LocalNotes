package dev.pinaki.localnotes.data

import java.util.Date

class NotesRepository(database: NotesDatabase) {
    private val noteDao = database.noteDao()

    suspend fun getAllNotes(): List<Note> = noteDao.getAll()

    suspend fun getNote(id: Int): Note? = noteDao.getById(id)

    suspend fun createNote(title: String, content: String): Note {
        val now = Date()
        val note = Note(
            title = title,
            content = content,
            dateAdded = now,
            dateModified = now,
        )
        val id = noteDao.insert(note)

        return note.copy(id = id.toInt())
    }

    suspend fun updateNote(note: Note): Boolean =
        noteDao.update(note.copy(dateModified = Date())) > 0

    suspend fun deleteNote(id: Int): Boolean = noteDao.deleteById(id) > 0
}
