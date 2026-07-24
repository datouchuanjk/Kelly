package io.kelly.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.util.concurrent.ConcurrentHashMap

internal val createdTables = ConcurrentHashMap.newKeySet<String>()
internal val tablesType = ConcurrentHashMap<String, Map<String, String>>()

internal fun SQLiteDatabase.createTable(
    tableName: String,
    schema: Map<String, String>
) {
    if (createdTables.contains(tableName) || tableIsExists(tableName)) {
        createdTables.add(tableName)
        if (!tablesType.containsKey(tableName)) {
            tablesType[tableName] = queryTableSchema(tableName)
        }
        return
    }
    val value = schema.toList().joinToString(", ") { (name, type) ->
        "`$name` ${type.uppercase()}"
    }
    val sql = "CREATE TABLE IF NOT EXISTS `$tableName` ($value);"
    execSQL(sql)
    createdTables.add(tableName)
    tablesType[tableName] = schema
}

internal fun SQLiteDatabase.tableIsExists(tableName: String): Boolean {
    val sql = "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?"
    val cursor = rawQuery(sql, arrayOf(tableName))
    return cursor.use { it.count > 0 }
}

internal fun SQLiteDatabase.migrateTable(
    tableName: String,
    oldSchema: Map<String, String>,
    newSchema: Map<String, String>
): Boolean {
    val tempTableName = "${tableName}_temp"
    beginTransaction()
    try {
        execSQL("DROP TABLE IF EXISTS `$tempTableName`")
        execSQL("ALTER TABLE `$tableName` RENAME TO `$tempTableName`")
        createdTables.remove(tableName)
        createTable(tableName, newSchema)
        var page = 1
        val pageSize = 100
        val newKeys = newSchema.keys
        while (true) {
            val oldRows = query(tempTableName) {
                page(page, pageSize)
            }
            if (oldRows.isEmpty()) break
            for (row in oldRows) {
                val values = ContentValues()
                row.forEach {
                    val oldName = it.key
                    val oldValue = it.value
                    val oldType = oldSchema[oldName]
                    val newType = newSchema[oldName]
                    if (oldName in newKeys) {
                        if (oldValue == null) {
                            values.putNull(oldName)
                            return@forEach
                        }
                        when (newType) {
                            "TEXT" -> {
                                values.put(oldName, oldValue.toString())
                            }

                            "REAL" -> {
                                if (oldType in arrayOf("TEXT")) {
                                    val toDoubleOrNull = oldValue.toString().toDoubleOrNull()
                                    if (toDoubleOrNull != null) {
                                        values.put(oldName, toDoubleOrNull)
                                    }
                                }
                                if (oldType in arrayOf("REAL", "INTEGER")) {
                                    values.put(oldName, oldValue.toString().toDouble())
                                }
                            }

                            "INTEGER" -> {
                                if (oldType in arrayOf("TEXT")) {
                                    val toLongOrNull = oldValue.toString().toLongOrNull()
                                    if (toLongOrNull != null) {
                                        values.put(oldName, toLongOrNull)
                                    }
                                }
                                if (oldType in arrayOf("REAL", "INTEGER")) {
                                    values.put(oldName, oldValue.toString().toDouble().toLong())
                                }
                            }

                            "BOOLEAN" -> {
                                if (oldType in arrayOf("BOOLEAN")) {
                                    values.put(oldName, oldValue.toString())
                                }
                            }

                            "BLOB" -> {
                                when (oldValue) {
                                    is ByteArray -> {
                                        values.put(oldName, oldValue)
                                    }

                                    is String -> {
                                        values.put(oldName, oldValue.toByteArray(Charsets.UTF_8))
                                    }

                                    else -> {
                                        values.putNull(oldName)
                                    }
                                }
                            }

                            else -> {
                                values.putNull(oldName)
                            }
                        }
                    }
                }
                insert(tableName, null, values)
            }
            page++
        }
        execSQL("DROP TABLE IF EXISTS `$tempTableName`")
        createdTables.remove(tempTableName)
        setTransactionSuccessful()
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    } finally {
        endTransaction()
    }
}

internal fun SQLiteDatabase.addColumnToTable(
    tableName: String,
    oldSchema: Map<String, String>,
    newSchema: Map<String, String>
) {
    val newCols = newSchema.keys - oldSchema.keys
    beginTransaction()
    try {
        newCols.forEach { colName ->
            val colType = newSchema[colName].orEmpty().uppercase()
            execSQL("ALTER TABLE `$tableName` ADD COLUMN `$colName` $colType;")
        }
        setTransactionSuccessful()
        tablesType[tableName] = newSchema
    } finally {
        endTransaction()
    }
}

internal fun SQLiteDatabase.queryTableSchema(
    tableName: String,
): Map<String, String> {
    if (!createdTables.contains(tableName) && !tableIsExists(tableName)) {
        return emptyMap()
    }
    val map = mutableMapOf<String, String>()
    rawQuery(
        "PRAGMA table_info(`$tableName`)",
        null
    ).use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        val typeIndex = cursor.getColumnIndex("type")
        if (nameIndex != -1 && typeIndex != -1) {
            while (cursor.moveToNext()) {
                val colName = cursor.getString(nameIndex)
                val colType = cursor.getString(typeIndex)
                map[colName] = colType
            }
        }
    }
    return map
}

internal fun SQLiteDatabase.dropTable(tableName: String): Boolean {
    return try {
        execSQL("DROP TABLE IF EXISTS `$tableName`;")
        createdTables.remove(tableName)
        tablesType.remove(tableName)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
