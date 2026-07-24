package io.kelly.extension

import org.json.JSONArray
import org.json.JSONObject

fun String.toJsonObjectOrNull() = tryResult {
    JSONObject(this)
}.getOrNull()

fun String.toJsonArrayOrNull() = tryResult {
    JSONArray(this)
}.getOrNull()

fun Map<String, String>.toJsonObjectOrNull() = tryResult {
    JSONObject(this)
}.getOrNull()

