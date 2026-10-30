package com.bibletriviaquiz.playandwin.data.repository

import android.content.Context
import com.bibletriviaquiz.playandwin.R
import com.bibletriviaquiz.playandwin.model.CashRedemptionRequest
import com.bibletriviaquiz.playandwin.model.Order
import com.bibletriviaquiz.playandwin.model.Product
import com.bibletriviaquiz.playandwin.model.Question
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

class AdminRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun isCurrentUserAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        val email = user.email?.lowercase() ?: ""
        return email == "ubyytech2023@gmail.com"
    }

    suspend fun addQuestion(
        text: String,
        options: List<String>,
        correctIndex: Int,
        category: String,
        difficulty: String,
        reference: String?,
        explanation: String?
    ): Question {
        val questionId = "q_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val docRef = db.collection("questions").document(questionId)

        val payload = hashMapOf<String, Any>(
            "questionId" to questionId,
            "questionText" to text.trim(),
            "options" to options.map { it.trim() },
            "correctOptionIndex" to correctIndex.toLong(),
            "category" to category,
            "difficulty" to difficulty,
            "isPublished" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!reference.isNullOrBlank()) payload["bibleReference"] = reference.trim()
        if (!explanation.isNullOrBlank()) payload["explanation"] = explanation.trim()

        try {
            docRef.set(payload).await()
            return Question(
                id = questionId,
                questionId = questionId,
                questionText = text,
                options = options,
                correctOptionIndex = correctIndex.toLong(),
                category = category,
                difficulty = difficulty,
                bibleReference = reference,
                explanation = explanation,
                isPublished = true
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun batchUploadQuestions(questions: List<Question>): Result<Int> {
        if (questions.isEmpty()) return Result.success(0)

        return try {
            val chunks = questions.chunked(400)
            var totalSaved = 0

            for (chunk in chunks) {
                val batch = db.batch()
                for (q in chunk) {
                    val qId = if (q.questionId.isNotBlank()) q.questionId else "q_${UUID.randomUUID().toString().replace("-", "").take(14)}"
                    val docRef = db.collection("questions").document(qId)
                    val payload = hashMapOf<String, Any>(
                        "questionId" to qId,
                        "questionText" to q.questionText.trim(),
                        "options" to q.options.map { it.trim() },
                        "correctOptionIndex" to q.correctOptionIndex,
                        "category" to q.category.trim(),
                        "difficulty" to q.difficulty.ifBlank { "Medium" },
                        "isPublished" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    if (!q.bibleReference.isNullOrBlank()) payload["bibleReference"] = q.bibleReference!!.trim()
                    if (!q.explanation.isNullOrBlank()) payload["explanation"] = q.explanation!!.trim()

                    batch.set(docRef, payload)
                }
                batch.commit().await()
                totalSaved += chunk.size
            }

            Result.success(totalSaved)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "questions")
            Result.failure(e)
        }
    }

    fun observeAllQuestions(): Flow<List<Question>> = flow {
        val query = db.collection("questions")
        query.snapshots().map { snapshot ->
            snapshot.toObjects(Question::class.java).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "questions")
            throw e
        }.collect { emit(it) }
    }

    suspend fun addOrUpdateProduct(
        productId: String?,
        name: String,
        description: String,
        category: String,
        pointsPrice: Long,
        stock: Long,
        imageUrl: String
    ): Product {
        val id = productId ?: "prod_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("products").document(id)

        val payload = hashMapOf<String, Any>(
            "productId" to id,
            "name" to name.trim(),
            "description" to description.trim(),
            "category" to category,
            "pointsPrice" to pointsPrice,
            "stockQuantity" to stock,
            "imageUrl" to (if (imageUrl.isNotBlank()) imageUrl.trim() else "https://images.unsplash.com/photo-1544717305-2782549b5136?w=600"),
            "isPublished" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            return Product(
                id = id,
                productId = id,
                name = name,
                description = description,
                category = category,
                pointsPrice = pointsPrice,
                stockQuantity = stock,
                imageUrl = payload["imageUrl"] as String,
                isPublished = true
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateOrderStatus(
        orderId: String,
        status: String,
        trackingReference: String?,
        courierName: String?
    ) {
        val docRef = db.collection("orders").document(orderId)
        val updates = hashMapOf<String, Any>(
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!trackingReference.isNullOrBlank()) updates["trackingReference"] = trackingReference.trim()
        if (!courierName.isNullOrBlank()) updates["courierName"] = courierName.trim()

        try {
            docRef.update(updates).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeAllOrders(): Flow<List<Order>> = flow {
        val query = db.collection("orders")
        query.snapshots().map { snapshot ->
            snapshot.toObjects(Order::class.java).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "orders")
            throw e
        }.collect { emit(it) }
    }

    fun observeAllUsers(): Flow<List<com.bibletriviaquiz.playandwin.model.UserProfile>> = flow {
        val query = db.collection("users")
        query.snapshots().map { snapshot ->
            snapshot.toObjects(com.bibletriviaquiz.playandwin.model.UserProfile::class.java).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "users")
            throw e
        }.collect { emit(it) }
    }

    fun calculateDaysActive(createdAt: com.google.firebase.Timestamp?): Long {
        val created = createdAt?.toDate()?.time ?: return 0L
        val now = System.currentTimeMillis()
        val diffMs = (now - created).coerceAtLeast(0L)
        return diffMs / (1000L * 60 * 60 * 24)
    }

    fun observeAllCashRedemptions(): Flow<List<CashRedemptionRequest>> = flow {
        val query = db.collection("cashRedemptionRequests")
        query.snapshots().map { snapshot ->
            snapshot.toObjects(CashRedemptionRequest::class.java).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "cashRedemptionRequests")
            throw e
        }.collect { emit(it) }
    }

    /**
     * Observes every withdrawal request combined with the user's live profile,
     * progress (quizzes taken/passed), bonus points from referrals, and account details.
     */
    fun observeAllWithdrawalsDetailed(): Flow<List<com.bibletriviaquiz.playandwin.model.AdminWithdrawalDetail>> = flow {
        kotlinx.coroutines.flow.combine(
            observeAllCashRedemptions(),
            observeAllUsers()
        ) { requests, users ->
            val userMap = users.associateBy { it.userId }
            requests.map { req ->
                val profile = userMap[req.userId]
                val days = calculateDaysActive(profile?.createdAt)
                val bonusPts = (profile?.successfulReferrals ?: 0L) * 100L
                val quizPts = (profile?.totalQuizzesPassed ?: 0L) * 50L
                val completed = profile?.totalQuizzesCompleted ?: 0L
                val passed = profile?.totalQuizzesPassed ?: 0L
                val rate = if (completed > 0) ((passed.toFloat() / completed.toFloat()) * 100).toInt() else 0

                com.bibletriviaquiz.playandwin.model.AdminWithdrawalDetail(
                    request = req,
                    userProfile = profile,
                    daysActive = days,
                    bonusPointsEarned = bonusPts,
                    quizPointsEarned = quizPts,
                    passRatePercent = rate,
                    isEligible60Days = days >= 60L
                )
            }
        }.collect { emit(it) }
    }

    /**
     * Approves payment and records payment reference / transaction hash.
     */
    suspend fun approvePayment(
        requestId: String,
        paymentReference: String,
        adminNotes: String? = null
    ): Result<Unit> {
        val docRef = db.collection("cashRedemptionRequests").document(requestId)
        val updates = hashMapOf<String, Any>(
            "status" to "processed",
            "paymentReference" to paymentReference.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!adminNotes.isNullOrBlank()) {
            updates["adminNotes"] = adminNotes.trim()
        }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Marks a withdrawal request as approved (verified, pending bank payout execution).
     */
    suspend fun markRequestApproved(
        requestId: String,
        adminNotes: String? = null
    ): Result<Unit> {
        val docRef = db.collection("cashRedemptionRequests").document(requestId)
        val updates = hashMapOf<String, Any>(
            "status" to "approved",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!adminNotes.isNullOrBlank()) {
            updates["adminNotes"] = adminNotes.trim()
        }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Rejects withdrawal request and refunds the user's deducted points back into their wallet.
     */
    suspend fun rejectWithdrawalAndRefund(
        requestId: String,
        reason: String
    ): Result<Unit> {
        val docRef = db.collection("cashRedemptionRequests").document(requestId)

        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(IllegalArgumentException("Withdrawal request not found."))
            }

            val currentStatus = snapshot.getString("status") ?: "pending"
            if (currentStatus == "rejected") {
                return Result.failure(IllegalStateException("This request is already rejected."))
            }

            val userId = snapshot.getString("userId") ?: return Result.failure(IllegalStateException("User ID missing."))
            val pointsRedeemed = snapshot.getLong("pointsRedeemed") ?: 50000L

            // 1. Update withdrawal status to rejected
            val updates = hashMapOf<String, Any>(
                "status" to "rejected",
                "paymentReference" to "Rejected: ${reason.trim()}",
                "adminNotes" to reason.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()

            // 2. Refund points back to user's wallet
            val userRef = db.collection("users").document(userId)
            userRef.update(
                mapOf(
                    "currentPoints" to FieldValue.increment(pointsRedeemed),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // 3. Record refund transaction in pointTransactions ledger
            val txId = "tx_refund_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val txRef = db.collection("pointTransactions").document(txId)
            val txPayload = hashMapOf(
                "transactionId" to txId,
                "userId" to userId,
                "type" to "refund_restoration",
                "amount" to pointsRedeemed,
                "description" to "Refund: Cash redemption request #$requestId rejected (${reason.trim()})",
                "createdAt" to FieldValue.serverTimestamp()
            )
            txRef.set(txPayload).await()

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCashRedemptionStatus(
        requestId: String,
        status: String,
        paymentReference: String?
    ) {
        val docRef = db.collection("cashRedemptionRequests").document(requestId)
        val updates = hashMapOf<String, Any>(
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!paymentReference.isNullOrBlank()) updates["paymentReference"] = paymentReference.trim()

        try {
            docRef.update(updates).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }
}
