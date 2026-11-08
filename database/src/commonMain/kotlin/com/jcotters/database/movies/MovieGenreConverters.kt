package com.jcotters.database.movies

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class DbMovieGenre(val id: Int, val name: String)

class MovieGenreConverters {
    @TypeConverter
    fun encode(genres: List<DbMovieGenre>?): String? = genres?.let {
        buildJsonArray {
            it.forEach { genre ->
                add(buildJsonObject {
                    put("id", genre.id)
                    put("name", JsonPrimitive(genre.name))
                })
            }
        }.toString()
    }

    @TypeConverter
    fun decode(value: String?): List<DbMovieGenre>? = value?.let {
        Json.parseToJsonElement(it).jsonArray.map { element ->
            val genre = element.jsonObject
            DbMovieGenre(
                id = genre.getValue("id").jsonPrimitive.int,
                name = genre.getValue("name").jsonPrimitive.content,
            )
        }
    }
}
