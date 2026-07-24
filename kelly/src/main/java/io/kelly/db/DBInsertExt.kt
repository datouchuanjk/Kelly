package io.kelly.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import io.kelly.extension.onFinally
import io.kelly.extension.tryResult

internal fun SQLiteDatabase.insert(
    tableName: String,
    block: InsertDsl.() -> Unit
): Long {
    val dsl = InsertDsl().apply(block)
    createTable(tableName, dsl.typeMap)
    dsl.checkType(tableName, this)
    return insert(tableName, null, dsl.contentValues)
}

internal fun SQLiteDatabase.inserts(
    tableName: String,
    block: MultipleInsertDsl.() -> Unit
): List<Long> {
    val dsl = MultipleInsertDsl().apply(block)
    val ids = mutableListOf<Long>()
    if (dsl.rows.isEmpty()) return ids
    val combinedIncomingSchema = mutableMapOf<String, String>()
    dsl.rows.forEach { row ->
        combinedIncomingSchema.putAll(row.typeMap)
    }
    createTable(tableName, combinedIncomingSchema)
    val schemaChecker = InsertDsl().apply { typeMap.putAll(combinedIncomingSchema) }
    schemaChecker.checkType(tableName, this)
    beginTransaction()
    tryResult {
        dsl.rows.forEach { row ->
            val id = insertOrThrow(tableName, null, row.contentValues)
            ids.add(id)
        }
        setTransactionSuccessful()
    }.onFinally {
        endTransaction()
    }
    return ids
}

class InsertDsl internal constructor() {

    internal val typeMap = mutableMapOf<String, String>()
    internal val contentValues = ContentValues()

    infix fun <T> Column<T>.inject(value: T?) {
        if (name.isEmpty()) {
            throw NullPointerException("name is null")
        }
        if (type.isEmpty()) {
            throw NullPointerException("type is null")
        }
        typeMap[name] = type
        contentValues.put(value)
    }

    internal fun checkType(tableName: String, db: SQLiteDatabase) {
        val oldType = tablesType[tableName].orEmpty()
        if (oldType.isEmpty()) {
            return
        }
        val incomingType = typeMap
        val allIncomingKeysTypeMatch = incomingType.all { (key, type) ->
            oldType[key] == type
        }
        if (allIncomingKeysTypeMatch) {
            return
        }
        val mergedSchema = oldType.toMutableMap().apply { putAll(incomingType) }
        val isSimpleAppend = oldType.all { (key, type) ->
            mergedSchema[key] == type
        }
        if (isSimpleAppend) {
            db.addColumnToTable(tableName, oldType, mergedSchema)
        } else {
            db.migrateTable(
                tableName = tableName,
                oldSchema = oldType,
                newSchema = mergedSchema
            )
        }
        typeMap.clear()
        typeMap.putAll(mergedSchema)
    }
}

class MultipleInsertDsl internal constructor() {
    internal val rows = mutableListOf<InsertDsl>()

    fun row(block: InsertDsl.() -> Unit) {
        rows.add(InsertDsl().apply(block))
    }
}