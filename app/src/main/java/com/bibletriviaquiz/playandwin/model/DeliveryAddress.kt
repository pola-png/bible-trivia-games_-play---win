package com.bibletriviaquiz.playandwin.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class DeliveryAddress(
    @DocumentId val id: String = "",
    val addressId: String = "",
    val userId: String = "",
    val recipientName: String = "",
    val phoneNumber: String = "",
    val state: String = "",
    val city: String = "",
    val streetAddress: String = "",
    val landmark: String? = null,
    val instructions: String? = null,
    val isDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
