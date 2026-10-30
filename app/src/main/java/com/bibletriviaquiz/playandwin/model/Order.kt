package com.bibletriviaquiz.playandwin.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val productImageUrl: String = "",
    val pointsSpent: Long = 10000L,
    val status: String = "pending", // "pending", "confirmed", "processing", "dispatched", "delivered", "cancelled"
    val trackingReference: String? = null,
    val courierName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
