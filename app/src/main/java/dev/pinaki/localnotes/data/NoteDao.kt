package dev.pinaki.localnotes.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY dateModified DESC")
    suspend fun getAll(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Int): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note): Int

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Int): Int
}
