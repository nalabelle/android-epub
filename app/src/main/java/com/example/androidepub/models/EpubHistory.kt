package com.example.androidepub.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "epub_history")
data class EpubHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val filePath: String,
    val createdAt: Date
)
