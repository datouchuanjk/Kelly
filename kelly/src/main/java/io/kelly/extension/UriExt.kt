package io.kelly.extension

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import io.kelly.KellyInternal

val Uri.id: Long get() = ContentUris.parseId(this)

fun Uri.appendedId(id: Long): Uri {
    return ContentUris.withAppendedId(this, id)
}

fun Uri.isAccessible(): Boolean {
    val id = tryResult { id }.getOrNull()
    if (id == null) return false
    return querySelf {
        addProjection(MediaStore.MediaColumns._ID)
        MediaStore.MediaColumns._ID eq id.toString()
    } != null
}

fun Uri.queryAll(
    block: MediaStoreQueryDsl.() -> Unit
): List<QueryResult> {
    val contentResolver = KellyInternal.context.contentResolver
    val queryDsl = MediaStoreQueryDsl().apply(block)
    return tryResult {
        contentResolver.query(this, queryDsl.projection, queryDsl.queryArgs(), null)
    }.getOrNull()?.readAll(
        this,
        queryDsl.projection,
        queryDsl.limit,
        queryDsl.offset
    ).orEmpty()
}

fun Uri.querySelf(block: MediaStoreQueryDsl.() -> Unit): QueryResult? {
    val contentResolver = KellyInternal.context.contentResolver
    val queryDsl = MediaStoreQueryDsl().apply(block)
    return tryResult {
        contentResolver.query(this, queryDsl.projection, null, null, null)
    }.getOrNull()?.readFirst(this, queryDsl.projection)
}

fun Uri.insert(block: ContentValues.() -> Unit): Uri? {
    val values = ContentValues().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }.apply(block)
    return KellyInternal.context.contentResolver.insert(this, values)
}

fun Uri.update(block: ContentValues.() -> Unit) {
    KellyInternal.context.contentResolver.update(
        this,
        ContentValues().apply(block), null, null
    )
}

fun Uri.updateToComplete() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        update {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
    }
}

fun Uri.delete() {
    KellyInternal.context.contentResolver.delete(this, null, null)
}

fun Uri.openOutputStream() = KellyInternal.context.contentResolver.openOutputStream(this)

fun Uri.openInputStream() = KellyInternal.context.contentResolver.openInputStream(this)

class MediaStoreQueryDsl internal constructor() {
    private val _projection = mutableListOf(MediaStore.MediaColumns._ID)
    private val _selection = mutableListOf<String>()
    private val _selectionArgs = mutableListOf<String>()
    private val _sortOrder = mutableListOf<String>()
    private var _page = 1
    private var _pageSize = 50

    fun addProjection(value: String) {
        _projection.add(value)
    }

    infix fun String.eq(value: String) {
        _selection.add("$this = ?")
        _selectionArgs.add(value)
    }

    infix fun String.like(value: String) {
        _selection.add("$this LIKE ?")
        _selectionArgs.add(value)
    }

    infix fun String.any(value: Array<String>) {
        _selection.add(
            "(${
                value.joinToString(" OR ") {
                    "$this = ?"
                }
            })"
        )
        _selectionArgs.addAll(value)
    }

    fun addSortOrderAsc(value: String) = _sortOrder.add("$value ASC")

    fun addSortOrderDesc(value: String) = _sortOrder.add("$value DESC")

    var page: Int
        @Deprecated("Write-only property", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        set(value) {
            _page = value
        }

    var pageSize: Int
        @Deprecated("Write-only property", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        set(value) {
            _pageSize = value
        }

    internal val projection get() = _projection.toTypedArray()

    internal val selection
        get() = if (_selection.isEmpty()) null else _selection.joinToString(" AND ")

    internal val selectionArgs
        get() = if (_selectionArgs.isEmpty()) null else _selectionArgs.toTypedArray()

    internal val sortOrder
        get() = if (_sortOrder.isEmpty()) null else _sortOrder.joinToString(", ")

    internal val limit get() = _pageSize

    internal val offset get() = (_page - 1) * _pageSize

    internal fun queryArgs(): Bundle {
        val bundle = Bundle()
        bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        bundle.putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
        val sel = selection
        if (sel != null) {
            bundle.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, sel)
            bundle.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
        }
        val sort = sortOrder
        if (sort != null) {
            bundle.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sort) //这李怎么 dsc 和 asc
        }
        return bundle
    }
}