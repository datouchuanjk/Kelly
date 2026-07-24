package io.kelly.extension

import java.net.URL

fun String.filterChinese(): String {
    return filter { it.isChinese() }
}

fun String.filterEnglishOrDigit(): String {
    return filter { it.isEnglish() || it.isDigit() }
}

fun String.filterEnglish(): String {
    return filter { it.isEnglish() }
}

fun String.filterDigit(): String {
    return filter { it.isDigit() }
}

fun Char.isEnglish() = this in 'a'..'z' || this in 'A'..'Z'

fun Char.isChinese() = this in '\u4e00'..'\u9fa5'

fun String.toURLOrNull() = tryResult {
    URL(this)
}.getOrNull()





