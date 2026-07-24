package io.kelly.extension

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

fun Any?.formatAmount(
    useGrouping: Boolean = false,
    decimalPlaces: Int = 0
): String {
    val value = toString().toDoubleOrNull() ?: return toString()
    val symbols = DecimalFormatSymbols(Locale.ENGLISH)
        .apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
    val df = DecimalFormat()
        .apply {
            decimalFormatSymbols = symbols
            isGroupingUsed = useGrouping
            groupingSize = 3
            maximumFractionDigits = decimalPlaces
            minimumFractionDigits = decimalPlaces
            roundingMode = RoundingMode.HALF_UP
        }
    return df.format(value)
}

class FileSize internal constructor(val b: Long) : Comparable<FileSize> {
    val kb: Double get() = b / 1024.0
    val mb: Double get() = b / (1024.0 * 1024.0)
    val gb: Double get() = b / (1024.0 * 1024.0 * 1024.0)
    operator fun plus(other: FileSize) = FileSize(b + other.b)
    operator fun minus(other: FileSize) = FileSize(b - other.b)
    operator fun times(other: Long) = FileSize(b * other)
    operator fun div(other: Long) = FileSize(b / other)
    override fun compareTo(other: FileSize): Int = b.compareTo(other.b)
    override fun toString(): String {
        return when {
            b < 1024 -> "$b B"
            b < 1024 * 1024 -> "%.1f KB".format(kb)
            b < 1024 * 1024 * 1024 -> "%.1f MB".format(mb)
            else -> "%.1f GB".format(gb)
        }
    }
}

val Number.b: FileSize get() = FileSize(toLong())

val Number.kb: FileSize get() = FileSize(toLong() * 1024)

val Number.mb: FileSize get() = FileSize(toLong() * 1024 * 1024)

val Number.gb: FileSize get() = FileSize(toLong() * 1024 * 1024 * 1024)

