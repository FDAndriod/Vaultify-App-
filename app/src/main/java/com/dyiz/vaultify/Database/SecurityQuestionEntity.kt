package com.dyiz.vaultify.Database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_question_table")
data class SecurityQuestionEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int = 1,
    val question: String,
    val answer: String,
    val hint: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
