package dev.pinaki.localnotes.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY dateModified DESC")
    suspend fun getAll(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Int): Note?

    @Upsert
    suspend fun upsert(note: Note)

    @Delete
    suspend fun delete(note: Note)
}
