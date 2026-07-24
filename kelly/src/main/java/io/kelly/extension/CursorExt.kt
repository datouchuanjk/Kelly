package io.kelly.extension

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import kotlin.getOrDefault

internal fun Cursor.readFirst(
    contentUri: Uri,
    projection: Array<String>
): QueryResult? {
    return use { cursor ->
        if (cursor.moveToFirst()) cursor.readRow(contentUri, projection) else null
    }
}

internal fun Cursor.readAll(
    contentUri: Uri,
    projection: Array<String>,
    limit: Int,
    offset: Int
): List<QueryResult> {
    val rawList = use { cursor ->
        val list = mutableListOf<QueryResult>()
        while (cursor.moveToNext()) {
            cursor.readRow(contentUri, projection)?.let { list.add(it) }
        }
        list
    }
    val honoredArgs = tryResult { extras?.getStringArray(ContentResolver.EXTRA_HONORED_ARGS) }.getOrNull()?:emptyArray<String>()
    val limitHonored = ContentResolver.QUERY_ARG_LIMIT in honoredArgs
    val offsetHonored = ContentResolver.QUERY_ARG_OFFSET in honoredArgs
    val actualLimitHonored = limitHonored && rawList.size <= limit
    val actualOffsetHonored = offsetHonored && actualLimitHonored
    var resultList: List<QueryResult> = rawList
    if (offset > 0 && !actualOffsetHonored) {
        resultList = resultList.drop(offset)
    }
    if (limit > 0 && !actualLimitHonored) {
        resultList = resultList.take(limit)
    }
    return resultList
}

private fun Cursor.readRow(
    contentUri: Uri,
    projection: Array<String>
): QueryResult? {
    val map = projection.associateWith { name ->
        val index = tryResult { getColumnIndexOrThrow(name) }.getOrDefault(-1)
        if (index == -1 || isNull(index)) {
            null
        } else {
            when (getType(index)) {
                Cursor.FIELD_TYPE_NULL -> null
                Cursor.FIELD_TYPE_INTEGER -> getLong(index)
                Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
                Cursor.FIELD_TYPE_STRING -> getString(index)
                Cursor.FIELD_TYPE_BLOB -> getBlob(index)
                else -> getString(index)
            }
        }
    }
    val id = map[MediaStore.MediaColumns._ID] as? Long ?: return null
    val cleanMap = map.toMutableMap().apply {
        remove(MediaStore.MediaColumns._ID)
    }
    val finalUri = if (tryResult { contentUri.id }.isSuccess) {
        contentUri
    } else {
        contentUri.appendedId(id)
    }
    return QueryResult(
        id = id,
        uri = finalUri,
        map = cleanMap
    )
}

data class QueryResult(
    val id: Long,
    val uri: Uri,
    @PublishedApi
    internal val map: Map<String, Any?>
) {
    inline fun <reified T> getOrNull(name: String): T? = map[name] as? T
    val title: String get() = getOrNull<String>(MediaStore.MediaColumns.TITLE).orEmpty()
    val artist: String get() = getOrNull<String>(MediaStore.MediaColumns.ARTIST).orEmpty()
    val duration: Long get() = getOrNull<Long>(MediaStore.MediaColumns.DURATION) ?: 0L
    val size: Long get() = getOrNull<Long>(MediaStore.MediaColumns.SIZE) ?: 0L
}

