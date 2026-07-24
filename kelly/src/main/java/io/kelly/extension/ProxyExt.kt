package io.kelly.extension

import java.lang.reflect.Proxy

inline fun <reified T : Any> noOp(): T {
    val javaClass = T::class.java
    return Proxy.newProxyInstance(
        javaClass.classLoader,
        arrayOf(javaClass)
    ) { _, _, _ -> } as T
}
