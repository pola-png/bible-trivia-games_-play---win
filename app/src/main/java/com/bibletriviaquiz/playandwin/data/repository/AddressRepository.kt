package com.bibletriviaquiz.playandwin.data.repository

import android.content.Context
import com.bibletriviaquiz.playandwin.R
import com.bibletriviaquiz.playandwin.model.DeliveryAddress
import com.bibletriviaquiz.playandwin.util.OperationType
import com.bibletriviaquiz.playandwin.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AddressRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeAddresses(): Flow<List<DeliveryAddress>> = flow {
        val uid = requireUserId()
        val query = db.collection("deliveryAddresses").whereEqualTo("userId", uid)
        query.snapshots().map { snapshot ->
            snapshot.toObjects(DeliveryAddress::class.java)
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "deliveryAddresses")
            throw e
        }.collect { emit(it) }
    }

    suspend fun saveAddress(
        recipientName: String,
        phoneNumber: String,
        state: String,
        city: String,
        streetAddress: String,
        landmark: String?,
        instructions: String?,
        isDefault: Boolean
    ): DeliveryAddress {
        val uid = requireUserId()
        val addressId = "addr_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val docRef = db.collection("deliveryAddresses").document(addressId)

        val payload = hashMapOf<String, Any>(
            "addressId" to addressId,
            "userId" to uid,
            "recipientName" to recipientName.trim(),
            "phoneNumber" to phoneNumber.trim(),
            "state" to state.trim(),
            "city" to city.trim(),
            "streetAddress" to streetAddress.trim(),
            "isDefault" to isDefault,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!landmark.isNullOrBlank()) payload["landmark"] = landmark.trim()
        if (!instructions.isNullOrBlank()) payload["instructions"] = instructions.trim()

        try {
            docRef.set(payload).await()
            return DeliveryAddress(
                id = addressId,
                addressId = addressId,
                userId = uid,
                recipientName = recipientName,
                phoneNumber = phoneNumber,
                state = state,
                city = city,
                streetAddress = streetAddress,
                landmark = landmark,
                instructions = instructions,
                isDefault = isDefault
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAddress(addressId: String) {
        val docRef = db.collection("deliveryAddresses").document(addressId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
