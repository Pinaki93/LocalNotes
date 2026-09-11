package dev.pinaki.localnotes.data

import android.content.Context
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(entities = [Note::class], version = 1)
@ColumnTypeConverters(DateConverters::class)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var instance: NotesDatabase? = null

        fun getInstance(context: Context): NotesDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder<NotesDatabase>(
                    context.applicationContext,
                    "notes.db",
                )
                    .setDriver(AndroidSQLiteDriver())
                    .build()
                    .also { instance = it }
            }
    }
}
