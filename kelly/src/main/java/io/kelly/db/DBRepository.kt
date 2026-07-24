package io.kelly.db

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.kelly.KellyInternal

val dbRepository: DBRepository by lazy { DBRepositoryImpl }

interface DBRepository {
    fun insert(
        tableName: String,
        block: InsertDsl.() -> Unit
    ): Long

    fun inserts(
        tableName: String,
        block: MultipleInsertDsl.() -> Unit
    ): List<Long>

    fun query(
        tableName: String,
        block: QueryDsl.() -> Unit = {}
    ): List<Map<String, Any?>>

    fun dropTable(tableName: String): Boolean

    fun delete(
        tableName: String,
        block: DeleteDsl.() -> Unit = {}
    ): Boolean
}

internal object DBRepositoryImpl : DBRepository {
    private val _helper by lazy { SQLHelper() }
    private val writableDatabase: SQLiteDatabase get() = _helper.writableDatabase
    private val readableDatabase: SQLiteDatabase get() = _helper.readableDatabase

    override fun insert(
        tableName: String,
        block: InsertDsl.() -> Unit
    ): Long {
        return writableDatabase.insert(tableName, block)
    }

    override fun inserts(
        tableName: String,
        block: MultipleInsertDsl.() -> Unit
    ): List<Long> {
        return writableDatabase.inserts(tableName, block)
    }

    override fun query(
        tableName: String,
        block: QueryDsl.() -> Unit
    ): List<Map<String, Any?>> {
        return readableDatabase.query(tableName, block)
    }

    override fun dropTable(tableName: String): Boolean {
        return writableDatabase.dropTable(tableName)
    }

    override fun delete(
        tableName: String,
        block: DeleteDsl.() -> Unit
    ): Boolean {
        return writableDatabase.delete(tableName, block)
    }

    internal class SQLHelper : SQLiteOpenHelper(
        KellyInternal.context,
        "KELLY_SQL_HELPER",
        null,
        1
    ) {
        override fun onConfigure(db: SQLiteDatabase) {
            super.onConfigure(db)
            db.enableWriteAheadLogging()
        }

        override fun onCreate(db: SQLiteDatabase?) {
        }

        override fun onUpgrade(
            db: SQLiteDatabase?,
            oldVersion: Int,
            newVersion: Int
        ) {
        }
    }
}