package io.kelly.composable.wheel.linkage.provider

import io.kelly.extension.findIndexOrNull
import java.time.LocalDate
import java.time.YearMonth

internal class LocalDateProvider(
    private val list: List<Int> = (1970..LocalDate.now().year).toList(),
    private val useCurrentDate: Boolean = true,
    private val currentMap: (Int, Int) -> String = { _, item -> item.toString() }
) : LinkageProvider<Int> {
    private val localDate: LocalDate? = LocalDate.now()
    override fun initialPrimaryIndex(list: List<Int>): Int {
        return if (useCurrentDate) {
            list.findIndexOrNull { it == localDate?.year } ?: 0
        } else 0
    }

    override fun initialSecondaryIndex(list: List<Int>): Int {
        return if (useCurrentDate) {
            list.findIndexOrNull { it == localDate?.month?.value } ?: 0
        } else 0
    }

    override fun initialTertiaryIndex(list: List<Int>): Int {
        return if (useCurrentDate) {
            list.findIndexOrNull { it == localDate?.dayOfMonth } ?: 0
        } else 0
    }

    override fun map(item: Int, level: Int): String {
        return currentMap(level, item)
    }

    override fun providePrimary(): List<Int> {
        return list
    }

    override fun provideSecondary(primary: Int): List<Int> {
        return (1..12).toList()
    }

    override fun provideTertiary(primary: Int, secondary: Int?): List<Int> {
        if (secondary == null) {
            return emptyList()
        }
        return (1..YearMonth.of(primary, secondary).lengthOfMonth()).toList()
    }
}