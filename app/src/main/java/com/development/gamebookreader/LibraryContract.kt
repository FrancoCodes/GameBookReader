package com.development.gamebookreader

import android.provider.BaseColumns

object EbookTable {
    const val TABLE_NAME = "ebooks"
    const val COLUMN_ID = "id"
    const val COLUMN_TITLE = "title"
    const val COLUMN_URI = "uri"
    const val COLUMN_LOCAL_URI = "local_uri" // Add the new column for local URIs

    const val CREATE_TABLE = "CREATE TABLE $TABLE_NAME (" +
            "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
            "$COLUMN_TITLE TEXT," +
            "$COLUMN_URI TEXT," +
            "$COLUMN_LOCAL_URI TEXT)" // Include the new column in the CREATE_TABLE statement
}

