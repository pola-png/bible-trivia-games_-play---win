package com.bibletriviaquiz.playandwin.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ReferralCodeLookup(
    @DocumentId val id: String = "",
    val code: String = "",
    val userId: String = "",
    val displayName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
