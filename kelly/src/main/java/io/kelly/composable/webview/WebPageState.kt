package io.kelly.composable.webview

sealed interface WebPageState {
    data object Initial : WebPageState
    data class Loading(val progress: Float) : WebPageState
    data object Success : WebPageState
     open class Error(
        open val failingUrl: String?,
        open val description: String? = null
    ) : WebPageState {

        data class NetworkError(
            override val failingUrl: String?,
            val errorCode: Int?,
            override val description: String?
        ) : Error(failingUrl, description)

        data class HttpError(
            override val failingUrl: String?,
            val statusCode: Int?,
            override val description: String?
        ) : Error(failingUrl, description)
    }
}