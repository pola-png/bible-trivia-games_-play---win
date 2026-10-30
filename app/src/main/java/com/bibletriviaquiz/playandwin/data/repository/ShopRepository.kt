package com.bibletriviaquiz.playandwin.data.repository

import android.content.Context
import com.bibletriviaquiz.playandwin.R
import com.bibletriviaquiz.playandwin.model.DeliveryAddress
import com.bibletriviaquiz.playandwin.model.Order
import com.bibletriviaquiz.playandwin.model.Product
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

class ShopRepository(private val db: FirebaseFirestore) {

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

    val defaultSampleProducts: List<Product> = emptyList()

    fun observeProducts(): Flow<List<Product>> = flow {
        val query = db.collection("products").whereEqualTo("isPublished", true)
        query.snapshots().map { snapshot ->
            snapshot.toObjects(Product::class.java)
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "products")
            emit(emptyList())
        }.collect { emit(it) }
    }

    suspend fun redeemProduct(
        product: Product,
        address: DeliveryAddress
    ): Result<Order> {
        val uid = requireUserId()
        if (product.pointsPrice < 10000L) {
            return Result.failure(IllegalStateException("Minimum redemption price is 10,000 points."))
        }

        val userRef = db.collection("users").document(uid)
        val productRef = db.collection("products").document(product.productId)

        try {
            val userSnapshot = userRef.get().await()
            val currentPoints = userSnapshot.getLong("currentPoints") ?: 0L
            if (currentPoints < 10000L) {
                return Result.failure(IllegalStateException("You need at least 10,000 points to unlock redemptions."))
            }
            if (currentPoints < product.pointsPrice) {
                return Result.failure(IllegalStateException("Insufficient points. You have $currentPoints points, but this item costs ${product.pointsPrice} points."))
            }

            // Generate order ID and transaction ID
            val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val orderRef = db.collection("orders").document(orderId)
            val txRef = db.collection("pointTransactions").document(txId)

            val orderPayload = hashMapOf(
                "orderId" to orderId,
                "userId" to uid,
                "productId" to product.productId,
                "productName" to product.name,
                "productImageUrl" to product.imageUrl,
                "pointsSpent" to product.pointsPrice,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp()
            )

            val txPayload = hashMapOf(
                "transactionId" to txId,
                "userId" to uid,
                "type" to "product_redemption",
                "amount" to -product.pointsPrice,
                "description" to "Redeemed: ${product.name} (Order #$orderId)",
                "createdAt" to FieldValue.serverTimestamp()
            )

            // Deduct points
            userRef.update(
                mapOf(
                    "currentPoints" to FieldValue.increment(-product.pointsPrice),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Save order and transaction
            orderRef.set(orderPayload).await()
            txRef.set(txPayload).await()

            // Decrement stock if document exists
            try {
                productRef.update("stockQuantity", FieldValue.increment(-1L)).await()
            } catch (_: Exception) {}

            val createdOrder = Order(
                id = orderId,
                orderId = orderId,
                userId = uid,
                productId = product.productId,
                productName = product.name,
                productImageUrl = product.imageUrl,
                pointsSpent = product.pointsPrice,
                status = "pending"
            )

            return Result.success(createdOrder)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "orders")
            return Result.failure(e)
        }
    }

    fun observeOrders(): Flow<List<Order>> = flow {
        val uid = requireUserId()
        val query = db.collection("orders").whereEqualTo("userId", uid)
        query.snapshots().map { snapshot ->
            snapshot.toObjects(Order::class.java).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "orders")
            throw e
        }.collect { emit(it) }
    }
}
