package io.kelly.composable.wheel.linkage.provider

internal class SingleProvider<T>(
    private val list: List<T>,
    private val currentMap: (T) -> String = { it.toString() }
) : LinkageProvider<T> {
    override fun map(item: T, level: Int): String {
        return currentMap(item)
    }

    override fun providePrimary(): List<T> {
        return list
    }
}
