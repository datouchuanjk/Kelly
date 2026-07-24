package io.kelly.composable.wheel.linkage.provider

import io.kelly.KellyInternal
import java.time.LocalDate

interface LinkageProvider<T> {
    val context get() = KellyInternal.context
    fun map(item: T, level: Int): String {
        return item.toString()
    }

    fun initialPrimaryIndex(list: List<T>): Int {
        return 0
    }

    fun initialSecondaryIndex(list: List<T>): Int {
        return 0
    }

    fun initialTertiaryIndex(list: List<T>): Int {
        return 0
    }

    fun providePrimary(): List<T>

    fun provideSecondary(primary: T): List<T> = emptyList()

    fun provideTertiary(primary: T, secondary: T?): List<T> = emptyList()

    companion object {
        fun <T> singe(
            list: List<T>,
            map: ( T) -> String = {it.toString() }
        ): LinkageProvider<T> {
            return SingleProvider(list, map)
        }

        fun localDate(
            list: List<Int> = (1970..LocalDate.now().year).toList(),
            useCurrentDate: Boolean = true,
            map: (Int, Int) -> String = { _, item -> item.toString() },
        ): LinkageProvider<Int> {
            return LocalDateProvider(list, useCurrentDate, map)
        }
    }
}