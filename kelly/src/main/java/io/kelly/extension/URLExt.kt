package io.kelly.extension

import java.io.FilterInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

fun URL.inputStream(
    connectTimeout: Int = 10000,
    readTimeout: Int = 30000,
): InputStream {
    return connection("GET", connectTimeout, readTimeout, false) {
        object : FilterInputStream(it.inputStream) {
            override fun close() {
                tryResult {
                    super.close()
                }.onFinally {
                    it.disconnect()
                }
            }
        }
    }
}

fun <T> URL.head(
    connectTimeout: Int = 10000,
    readTimeout: Int = 10000,
    block: (HttpURLConnection) -> T
): T {
    return connection("HEAD", connectTimeout, readTimeout, true, block)
}

private inline fun <T> URL.connection(
    requestMethod: String,
    connectTimeout: Int = 10_000,
    readTimeout: Int = 10_000,
    autoDisconnect: Boolean = true,
    block: (HttpURLConnection) -> T,
): T {
    var connection: HttpURLConnection? = null
    return tryResult {
        connection = (openConnection() as HttpURLConnection).apply {
            this.requestMethod = requestMethod
            this.connectTimeout = connectTimeout
            this.readTimeout = readTimeout
        }
        block(connection)
    }.onFinally {
        if (autoDisconnect) {
            connection?.disconnect()
        }
    }.getOrThrow()
}