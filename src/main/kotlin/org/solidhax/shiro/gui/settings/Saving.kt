package org.solidhax.shiro.gui.settings

import com.google.gson.Gson
import com.google.gson.JsonElement

interface Saving {

    fun read(element: JsonElement, gson: Gson)

    fun write(gson: Gson): JsonElement
}
