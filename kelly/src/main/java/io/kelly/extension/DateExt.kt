package io.kelly.extension

import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object DateFormats {
    private val cache = ConcurrentHashMap<String, DateTimeFormatter>()
    val YMD = of("yyyy-MM-dd")
    val YMD_HMS = of("yyyy-MM-dd HH:mm:ss")
    fun of(pattern: String): DateFormat {
        val formatter = cache.getOrPut(pattern) {
            DateTimeFormatter.ofPattern(pattern)
        }
        return DateFormat {
            formatter.format(it)
        }
    }
}

fun interface DateFormat {
    fun format(temporal: TemporalAccessor): String
}

fun LocalDateTime.format(df: DateFormat): String {
    return df.format(this)
}

fun LocalDate.format(df: DateFormat): String {
    return df.format(this)
}

fun Long.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zoneId)
}

fun LocalDateTime.toEpochMilli(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    return atZone(zoneId).toInstant().toEpochMilli()
}

fun Duration.formatCountdown(
    dayStr: String = "d ",
    hourStr: String = "h ",
    minStr: String = "m ",
    secStr: String = "s",
): String {
    if (this <= Duration.ZERO) return "0$secStr"
    return toComponents { days, hours, minutes, seconds, _ ->
        buildString {
            if (days > 0) append(days).append(dayStr)
            if (hours > 0) append(hours).append(hourStr)
            if (minutes > 0) append(minutes).append(minStr)
            if (seconds > 0 || isEmpty()) {
                append(seconds).append(secStr)
            }
        }.trim()
    }
}

suspend fun Duration.onEachTick(block: (Duration) -> Unit) {
    var remaining = this
    while (remaining > 0.seconds) {
        delay(1000)
        block(remaining)
        remaining -= 1.seconds
    }
    block(0.seconds)
}


