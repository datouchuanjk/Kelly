package io.kelly.db

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

internal fun SQLiteDatabase.query(
    tableName: String,
    block: QueryDsl.() -> Unit = {}
): List<Map<String, Any?>> {
    if (!createdTables.contains(tableName)) {
        if (tableIsExists(tableName)) {
            createdTables.add(tableName)
            if (!tablesType.containsKey(tableName)) {
                tablesType[tableName] = queryTableSchema(tableName)
            }
        } else {
            return emptyList()
        }
    }

    val dsl = QueryDsl().apply(block)
    val sql = "SELECT * FROM `$tableName` ${dsl.buildWhereClause()} ${dsl.buildOrderBy()} ${dsl.buildLimitOffset()}"
    val cursor = rawQuery(sql, dsl.getArgs())
    val list = mutableListOf<Map<String, Any?>>()
    val tableSchema = tablesType[tableName]
    cursor.use {
        while (it.moveToNext()) {
            val rowMap = mutableMapOf<String, Any?>()
            for (index in 0 until it.columnCount) {
                val columnName = it.getColumnName(index)
                var value: Any? = when (it.getType(index)) {
                    Cursor.FIELD_TYPE_INTEGER -> it.getLong(index)
                    Cursor.FIELD_TYPE_FLOAT -> it.getDouble(index)
                    Cursor.FIELD_TYPE_STRING -> it.getString(index)
                    Cursor.FIELD_TYPE_BLOB -> it.getBlob(index)
                    Cursor.FIELD_TYPE_NULL -> null
                    else -> null
                }
                if (value is String && tableSchema?.get(columnName) == "BOOLEAN") {
                    value = value.toBoolean()
                }
                rowMap[columnName] = value
            }
            list.add(rowMap)
        }
    }
    return list
}

class QueryDsl internal constructor() {
    private val whereClauses = mutableListOf<String>()
    private val selectionArgs = mutableListOf<String>()
    private var orderBy: String? = null
    private var limit: Int? = null
    private var offset: Int? = null

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

    fun orderBy(columnName: String, ascending: Boolean = true) {
        this.orderBy = "ORDER BY `$columnName` ${if (ascending) "ASC" else "DESC"}"
    }

    fun page(pageIndex: Int, pageSize: Int) {
        val safePage = pageIndex.coerceAtLeast(1)
        this.limit = pageSize
        this.offset = (safePage - 1) * pageSize
    }

    internal fun buildWhereClause(): String {
        return if (whereClauses.isEmpty()) "" else "WHERE " + whereClauses.joinToString(" AND ")
    }

    internal fun buildOrderBy(): String {
        return orderBy ?: ""
    }

    internal fun buildLimitOffset(): String {
        var sql = ""
        limit?.let { sql += " LIMIT $it" }
        offset?.let { sql += " OFFSET $it" }
        return sql
    }

    internal fun getArgs(): Array<String>? =
        if (selectionArgs.isEmpty()) null else selectionArgs.toTypedArray()
}