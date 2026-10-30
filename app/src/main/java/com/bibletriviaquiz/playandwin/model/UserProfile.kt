package com.bibletriviaquiz.playandwin.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val currentPoints: Long = 0L,
    val totalQuizzesCompleted: Long = 0L,
    val totalQuizzesPassed: Long = 0L,
    val totalCorrectAnswers: Long = 0L,
    val referralCode: String = "",
    val referredByCode: String? = null,
    val referredByUserId: String? = null,
    val totalReferrals: Long = 0L,
    val successfulReferrals: Long = 0L,
    val categoryCycleRound: Long = 1L,
    val completedCategoriesInCycle: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
