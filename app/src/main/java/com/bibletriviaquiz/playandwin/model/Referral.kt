package com.bibletriviaquiz.playandwin.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Referral(
    @DocumentId val id: String = "",
    val referralId: String = "",
    val referrerUserId: String = "",
    val referrerCode: String = "",
    val refereeUserId: String = "",
    val refereeDisplayName: String = "",
    val refereeEmail: String = "",
    val refereePoints: Long = 0L,
    val targetPoints: Long = 10000L,
    val rewardPoints: Long = 100L,
    val status: String = "pending", // "pending" (<10,000 pts), "qualified" (>=10,000 pts), "credited" (reward given)
    @ServerTimestamp val creditedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isQualified: Boolean
        get() = refereePoints >= targetPoints

    val isCredited: Boolean
        get() = status == "credited"

    val progressFraction: Float
        get() = (refereePoints.toFloat() / targetPoints.toFloat()).coerceIn(0f, 1f)
}
