package com.bibletriviaquiz.playandwin.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Question(
    @DocumentId val id: String = "",
    val questionId: String = "",
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val correctOptionIndex: Long = 0L,
    val category: String = "",
    val difficulty: String = "Medium",
    val bibleReference: String? = null,
    val explanation: String? = null,
    val isPublished: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)
