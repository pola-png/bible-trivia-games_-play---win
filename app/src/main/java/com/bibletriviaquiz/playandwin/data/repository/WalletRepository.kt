package com.bibletriviaquiz.playandwin.data.repository

import android.content.Context
import com.bibletriviaquiz.playandwin.R
import com.bibletriviaquiz.playandwin.model.CashRedemptionRequest
import com.bibletriviaquiz.playandwin.model.PointTransaction
import com.bibletriviaquiz.playandwin.model.UserProfile
import com.bibletriviaquiz.playandwin.util.OperationType
import com.bibletriviaquiz.playandwin.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
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

class WalletRepository(private val db: FirebaseFirestore) {

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

    fun calculateDaysActive(createdAt: Timestamp?): Long {
        if (createdAt == null) return 0L
        val now = System.currentTimeMillis()
        val created = createdAt.toDate().time
        val diffMs = (now - created).coerceAtLeast(0L)
        return diffMs / (1000L * 60 * 60 * 24)
    }

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        docRef.snapshots().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }.collect { emit(it) }
    }

    fun observeTransactions(): Flow<List<PointTransaction>> = flow {
        val uid = requireUserId()
        val query = db.collection("pointTransactions")
            .whereEqualTo("userId", uid)
        query.snapshots().map { snapshot ->
            snapshot.toObjects(PointTransaction::class.java).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "pointTransactions")
            throw e
        }.collect { emit(it) }
    }

    fun observeCashRedemptionRequests(): Flow<List<CashRedemptionRequest>> = flow {
        val uid = requireUserId()
        val query = db.collection("cashRedemptionRequests")
            .whereEqualTo("userId", uid)
        query.snapshots().map { snapshot ->
            snapshot.toObjects(CashRedemptionRequest::class.java).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "cashRedemptionRequests")
            throw e
        }.collect { emit(it) }
    }

    suspend fun requestCashRedemption(
        pointsToRedeem: Long,
        accountName: String,
        accountNumber: String,
        bankName: String,
        payoutMethod: String
    ): Result<CashRedemptionRequest> {
        val uid = requireUserId()

        if (pointsToRedeem < 50000L) {
            return Result.failure(IllegalStateException("Minimum cash redemption threshold is 50,000 points ($50 USD)."))
        }

        val userRef = db.collection("users").document(uid)

        try {
            val userSnapshot = userRef.get().await()
            val currentPoints = userSnapshot.getLong("currentPoints") ?: 0L
            val createdAt = userSnapshot.getTimestamp("createdAt")

            val daysActive = calculateDaysActive(createdAt)
            if (daysActive < 60L) {
                val daysRemaining = 60L - daysActive
                return Result.failure(
                    IllegalStateException(
                        "You must be an active Bible Quiz app user for at least 60 days to request cash redemptions. " +
                        "Your account is $daysActive days old ($daysRemaining days remaining). " +
                        "This policy ensures our community is driven by genuine study of God's Word, not money."
                    )
                )
            }

            if (currentPoints < pointsToRedeem) {
                return Result.failure(
                    IllegalStateException(
                        "Insufficient points. You have %,d points, but requested %,d points.".format(currentPoints, pointsToRedeem)
                    )
                )
            }

            val usdAmount = pointsToRedeem / 1000L // 10,000 pts = $10 USD -> 50,000 pts = $50 USD
            val requestId = "cash_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(14)}"

            val requestRef = db.collection("cashRedemptionRequests").document(requestId)
            val txRef = db.collection("pointTransactions").document(txId)

            val cashPayload = hashMapOf(
                "requestId" to requestId,
                "userId" to uid,
                "pointsRedeemed" to pointsToRedeem,
                "usdAmount" to usdAmount,
                "accountName" to accountName.trim(),
                "accountNumber" to accountNumber.trim(),
                "bankName" to bankName.trim(),
                "payoutMethod" to payoutMethod.trim(),
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp()
            )

            val txPayload = hashMapOf(
                "transactionId" to txId,
                "userId" to uid,
                "type" to "cash_redemption",
                "amount" to -pointsToRedeem,
                "description" to "Cash Redemption Request #$requestId ($usdAmount USD)",
                "createdAt" to FieldValue.serverTimestamp()
            )

            // Deduct points atomically
            userRef.update(
                mapOf(
                    "currentPoints" to FieldValue.increment(-pointsToRedeem),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            requestRef.set(cashPayload).await()
            txRef.set(txPayload).await()

            return Result.success(
                CashRedemptionRequest(
                    id = requestId,
                    requestId = requestId,
                    userId = uid,
                    pointsRedeemed = pointsToRedeem,
                    usdAmount = usdAmount,
                    accountName = accountName,
                    accountNumber = accountNumber,
                    bankName = bankName,
                    payoutMethod = payoutMethod,
                    status = "pending"
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "cashRedemptionRequests")
            return Result.failure(e)
        }
    }

    suspend fun claimRewardedAdBonus(rewardPoints: Long = 25L): Result<Long> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val txRef = db.collection("pointTransactions").document(txId)

        val txPayload = hashMapOf(
            "transactionId" to txId,
            "userId" to uid,
            "type" to "rewarded_ad_bonus",
            "amount" to rewardPoints,
            "description" to "Bonus: Watched sponsored video ad (+%,d pts)".format(rewardPoints),
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            userRef.update(
                mapOf(
                    "currentPoints" to FieldValue.increment(rewardPoints),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            txRef.set(txPayload).await()
            Result.success(rewardPoints)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "pointTransactions")
            Result.failure(e)
        }
    }
}
