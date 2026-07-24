package io.kelly.source

import java.io.File
import java.util.concurrent.ConcurrentHashMap

private object PathLockManager {

    private class RefCountedLock {
        val lock = Any()
        var refCount = 0
    }

    private val locks = ConcurrentHashMap<String, RefCountedLock>()

    fun <T> withLock(path: String, block: () -> T): T {
        val refLock: RefCountedLock

        synchronized(locks) {
            refLock = locks.computeIfAbsent(path) { RefCountedLock() }
            refLock.refCount++
        }
        try {
            return synchronized(refLock.lock) {
                block()
            }
        } finally {
            synchronized(locks) {
                refLock.refCount--
                if (refLock.refCount == 0) {
                    locks.remove(path)
                }
            }
        }
    }
}

fun <T> File.withLock(block: () -> T): T {
    return PathLockManager.withLock(absolutePath, block)
}