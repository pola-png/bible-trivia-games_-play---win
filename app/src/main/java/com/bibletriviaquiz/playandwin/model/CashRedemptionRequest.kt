package com.bibletriviaquiz.playandwin.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CashRedemptionRequest(
    @DocumentId val id: String = "",
    val requestId: String = "",
    val userId: String = "",
    val pointsRedeemed: Long = 50000L,
    val usdAmount: Long = 50L,
    val accountName: String = "",
    val accountNumber: String = "",
    val bankName: String = "",
    val payoutMethod: String = "Bank Transfer",
    val status: String = "pending", // "pending", "approved", "processed", "rejected"
    val paymentReference: String? = null,
    val adminNotes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
