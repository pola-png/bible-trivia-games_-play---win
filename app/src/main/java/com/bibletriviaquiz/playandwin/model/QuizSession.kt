package com.bibletriviaquiz.playandwin.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class QuizSession(
    @DocumentId val id: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val category: String = "",
    val totalQuestions: Long = 30L,
    val correctAnswers: Long = 0L,
    val timedOutAnswers: Long = 0L,
    val isPassed: Boolean = false,
    val pointsAwarded: Long = 0L,
    val status: String = "in_progress", // "in_progress", "completed", "abandoned"
    @ServerTimestamp val createdAt: Timestamp? = null
)
