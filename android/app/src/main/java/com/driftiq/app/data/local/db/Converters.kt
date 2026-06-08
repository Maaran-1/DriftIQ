package com.driftiq.app.data.local.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? =
        value?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) }

    @TypeConverter
    fun toTimestamp(date: LocalDateTime?): Long? =
        date?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
}
