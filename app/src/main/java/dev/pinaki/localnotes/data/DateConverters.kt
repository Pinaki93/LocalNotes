package dev.pinaki.localnotes.data

import androidx.room3.ColumnTypeConverter
import java.util.Date

object DateConverters {
    @ColumnTypeConverter
    fun fromTimestamp(value: Long): Date = Date(value)

    @ColumnTypeConverter
    fun toTimestamp(date: Date): Long = date.time
}
