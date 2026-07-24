package io.kelly.remember.display

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.kelly.extension.mutable

internal class DisplayControllerImpl<T> : DisplayController<T> {
    override var data by mutableStateOf<T?>(null)
    override val parameters = mutableStateListOf<Any?>()
    override var isShow by mutableStateOf(false)

    override fun show(data: T, vararg params: Any?) {
        this.data = data
        parameters.mutable {
            clear()
            addAll(params)
        }
        isShow = true
    }

    override fun dismiss() {
        isShow = false
        data = null
        parameters.clear()
    }

}


