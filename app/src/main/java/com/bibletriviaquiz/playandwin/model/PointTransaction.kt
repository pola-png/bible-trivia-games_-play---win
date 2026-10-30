package com.bibletriviaquiz.playandwin.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PointTransaction(
    @DocumentId val id: String = "",
    val transactionId: String = "",
    val userId: String = "",
    val type: String = "quiz_reward", // "quiz_reward", "product_redemption", "cash_redemption", "refund_restoration", "admin_adjustment", "referral_bonus"
    val amount: Long = 0L,
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
