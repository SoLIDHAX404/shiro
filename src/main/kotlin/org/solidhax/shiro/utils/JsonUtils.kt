package org.solidhax.shiro.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.File

val prettyGson: Gson = GsonBuilder().setPrettyPrinting().create()

fun readJsonFile(file: File): JsonElement? {
    if (!file.exists()) return null
    val text = file.readText()
    return if (text.isBlank()) null else JsonParser.parseString(text)
}

fun writeJsonFile(file: File, json: JsonElement) {
    file.parentFile?.mkdirs()
    file.writeText(prettyGson.toJson(json))
}
