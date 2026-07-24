package io.kelly.db

import android.database.sqlite.SQLiteDatabase
import io.kelly.extension.tryResult

internal fun SQLiteDatabase.delete(
    tableName: String,
    block: DeleteDsl.() -> Unit = {}
): Boolean {
    if (!createdTables.contains(tableName) && !tableIsExists(tableName)) {
        return true
    }
    return tryResult {
        val dsl = DeleteDsl().apply(block)
        val whereClause = dsl.buildWhereClause()
        val args = dsl.getArgs()
        val sql = "DELETE FROM `$tableName` $whereClause"
        execSQL(sql, args.orEmpty())
        true
    }.getOrDefault(false)
}

class DeleteDsl internal constructor() {
    private val whereClauses = mutableListOf<String>()
    private val selectionArgs = mutableListOf<String>()

    infix fun String.eq(value: Any?) {
        if (value == null) {
            whereClauses.add("`$this` IS NULL")
        } else {
            whereClauses.add("`$this` = ?")
            selectionArgs.add(value.toString())
        }
    }

    infix fun String.like(value: String) {
        whereClauses.add("`$this` LIKE ?")
        selectionArgs.add(value)
    }


    internal fun buildWhereClause(): String {
        return if (whereClauses.isEmpty()) "" else "WHERE " + whereClauses.joinToString(" AND ")
    }


    internal fun getArgs(): Array<String>? =
        if (selectionArgs.isEmpty()) null else selectionArgs.toTypedArray()
}