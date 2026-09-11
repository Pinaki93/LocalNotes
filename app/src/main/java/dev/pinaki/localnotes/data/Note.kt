package dev.pinaki.localnotes.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.util.Date

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val dateAdded: Date = Date(),
    val dateModified: Date = dateAdded,
)
