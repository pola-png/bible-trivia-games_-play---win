package com.bibletriviaquiz.playandwin.data.repository

import android.content.Context
import com.bibletriviaquiz.playandwin.R
import com.bibletriviaquiz.playandwin.model.PointTransaction
import com.bibletriviaquiz.playandwin.model.Question
import com.bibletriviaquiz.playandwin.model.QuizSession
import com.bibletriviaquiz.playandwin.model.UserProfile
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
import com.bibletriviaquiz.playandwin.util.CategoryCycleManager
import com.bibletriviaquiz.playandwin.util.QuestionHistoryManager

class QuizRepository(private val db: FirebaseFirestore, private val context: Context? = null) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        context.applicationContext
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // Local question bank (returns empty list, questions stream from Firestore / verified procedural bank)
    fun getLocalQuestionBank(): List<Question> {
        return emptyList()
    }

    /**
     * Retrieves 30 questions for a category quiz session, strictly enforcing the rule
     * that questions do NOT repeat for at least 10 days of answering Bible quiz questions.
     * Pulls from the category's 100,000-question bank plus any admin-uploaded questions.
     */
    fun getQuizQuestionsForCategory(
        category: String,
        customQuestions: List<Question> = emptyList(),
        count: Int = 30
    ): List<Question> {
        val uid = auth.currentUser?.uid ?: "guest"
        val excludedIds = if (context != null) {
            QuestionHistoryManager.getRecentlyServedQuestionIds(context, uid)
        } else {
            emptySet()
        }
        val excludedTexts = if (context != null) {
            QuestionHistoryManager.getRecentlyServedQuestionTexts(context, uid)
        } else {
            emptySet()
        }

        val selected = ProceduralBibleQuestionBank.getQuestionsForCategory(
            category = category,
            count = count,
            excludedIds = excludedIds,
            excludedTexts = excludedTexts,
            customQuestions = customQuestions
        )

        // Record questions with current timestamp so neither ID nor text can be repeated for 10 days
        if (context != null && selected.isNotEmpty()) {
            QuestionHistoryManager.recordFullQuestionsServed(
                context = context,
                userId = uid,
                questions = selected
            )
        }

        return selected
    }

    /**
     * Returns the total question count available in the question bank for a category (always >= 100,000).
     */
    fun getCategoryQuestionCount(category: String, customQuestions: List<Question> = emptyList()): Int {
        val customCount = customQuestions.count { it.category.equals(category, ignoreCase = true) }
        return ProceduralBibleQuestionBank.getQuestionCountForCategory(category, customCount)
    }

    /**
     * Observes all questions in real-time directly from Firestore.
     */
    fun observeAllQuestions(): Flow<List<Question>> = flow {
        db.collection("questions")
            .whereEqualTo("isPublished", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Question::class.java)
            }
            .catch { e ->
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun createOrUpdateUserProfile(email: String, displayName: String) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        try {
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                val derivedCode = "REF-" + uid.replace(Regex("[^a-zA-Z0-9]"), "").uppercase().let { s ->
                    if (s.length >= 6) s.take(6) else s.padEnd(6, 'X')
                }
                val profile = hashMapOf(
                    "userId" to uid,
                    "email" to email,
                    "displayName" to displayName,
                    "currentPoints" to 0L,
                    "totalQuizzesCompleted" to 0L,
                    "totalQuizzesPassed" to 0L,
                    "totalCorrectAnswers" to 0L,
                    "referralCode" to derivedCode,
                    "totalReferrals" to 0L,
                    "successfulReferrals" to 0L,
                    "categoryCycleRound" to 1L,
                    "completedCategoriesInCycle" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(profile).await()

                try {
                    val codeRef = db.collection("referralCodes").document(derivedCode)
                    codeRef.set(
                        mapOf(
                            "code" to derivedCode,
                            "userId" to uid,
                            "displayName" to displayName,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            throw e
        }
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

    suspend fun saveCompletedQuizSession(
        category: String,
        correctAnswers: Int,
        timedOutAnswers: Int,
        questionIds: List<String> = emptyList()
    ): QuizSession {
        val uid = requireUserId()
        val isPassed = correctAnswers >= 20
        val pointsAwarded = if (isPassed) 50L else 0L
        val sessionId = "session_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val sessionRef = db.collection("quizSessions").document(sessionId)

        val sessionPayload = hashMapOf<String, Any>(
            "sessionId" to sessionId,
            "userId" to uid,
            "category" to category,
            "totalQuestions" to 30L,
            "correctAnswers" to correctAnswers.toLong(),
            "timedOutAnswers" to timedOutAnswers.toLong(),
            "isPassed" to isPassed,
            "pointsAwarded" to pointsAwarded,
            "status" to "completed",
            "nonRepeatWindowDays" to 10L,
            "questionBankCapacity" to 100000L,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (questionIds.isNotEmpty()) {
            sessionPayload["questionIds"] = questionIds
            if (context != null) {
                QuestionHistoryManager.recordQuestionsServed(context, uid, questionIds)
            }
        }

        try {
            sessionRef.set(sessionPayload).await()

            // Update stats & award points in users document
            val userRef = db.collection("users").document(uid)
            val userSnap = try { userRef.get().await() } catch (_: Exception) { null }
            val currentRound = userSnap?.getLong("categoryCycleRound") ?: 1L
            @Suppress("UNCHECKED_CAST")
            val currentCompleted = (userSnap?.get("completedCategoriesInCycle") as? List<String>) ?: emptyList()

            // Only progress to another stage/category if user achieves passing score (>= 20 out of 30)
            val cycleTransition = if (isPassed) {
                if (context != null) {
                    CategoryCycleManager.recordCategoryPlayed(
                        context = context,
                        userId = uid,
                        category = category,
                        currentRound = currentRound,
                        currentCompleted = currentCompleted
                    )
                } else {
                    val updated = if (CategoryCycleManager.isCategoryCompleted(category, currentCompleted)) {
                        currentCompleted
                    } else {
                        currentCompleted + category.trim()
                    }
                    val isAllCompleted = CategoryCycleManager.allCategories.all { cat ->
                        CategoryCycleManager.isCategoryCompleted(cat, updated)
                    }
                    CategoryCycleManager.CycleTransition(
                        isRoundCompleted = isAllCompleted,
                        newRound = if (isAllCompleted) currentRound + 1L else currentRound,
                        updatedCompletedCategories = if (isAllCompleted) emptyList() else updated,
                        previousRoundNumber = currentRound
                    )
                }
            } else {
                // When failing (< 20), do not mark category completed and do not progress to another stage
                CategoryCycleManager.CycleTransition(
                    isRoundCompleted = false,
                    newRound = currentRound,
                    updatedCompletedCategories = currentCompleted,
                    previousRoundNumber = currentRound
                )
            }

            val updates = hashMapOf<String, Any>(
                "totalQuizzesCompleted" to FieldValue.increment(1L),
                "totalCorrectAnswers" to FieldValue.increment(correctAnswers.toLong()),
                "categoryCycleRound" to cycleTransition.newRound,
                "completedCategoriesInCycle" to cycleTransition.updatedCompletedCategories,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isPassed) {
                updates["totalQuizzesPassed"] = FieldValue.increment(1L)
                updates["currentPoints"] = FieldValue.increment(pointsAwarded)
            }
            userRef.update(updates).await()

            // If passed, record transaction ledger entry and sync referral progress
            if (isPassed) {
                val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val txRef = db.collection("pointTransactions").document(txId)
                val txPayload = hashMapOf(
                    "transactionId" to txId,
                    "userId" to uid,
                    "type" to "quiz_reward",
                    "amount" to pointsAwarded,
                    "description" to "Passed $category quiz ($correctAnswers/30 correct)",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                txRef.set(txPayload).await()

                try {
                    val userSnap = userRef.get().await()
                    val totalPts = userSnap.getLong("currentPoints") ?: 0L
                    ReferralRepository(db).syncRefereePoints(totalPts)
                } catch (_: Exception) {}
            }

            return QuizSession(
                id = sessionId,
                sessionId = sessionId,
                userId = uid,
                category = category,
                totalQuestions = 30L,
                correctAnswers = correctAnswers.toLong(),
                timedOutAnswers = timedOutAnswers.toLong(),
                isPassed = isPassed,
                pointsAwarded = pointsAwarded,
                status = "completed"
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, sessionRef.path)
            throw e
        }
    }

    fun observeUserSessions(): Flow<List<QuizSession>> = flow {
        val uid = requireUserId()
        val query = db.collection("quizSessions").whereEqualTo("userId", uid)
        query.snapshots().map { snapshot ->
            snapshot.toObjects(QuizSession::class.java)
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "quizSessions")
            throw e
        }.collect { emit(it) }
    }

    suspend fun recordCategoryChosen(
        category: String,
        currentRound: Long,
        currentCompleted: List<String>
    ): CategoryCycleManager.CycleTransition {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)

        val cycleTransition = if (context != null) {
            CategoryCycleManager.recordCategoryPlayed(
                context = context,
                userId = uid,
                category = category,
                currentRound = currentRound,
                currentCompleted = currentCompleted
            )
        } else {
            val updated = if (CategoryCycleManager.isCategoryCompleted(category, currentCompleted)) {
                currentCompleted
            } else {
                currentCompleted + category.trim()
            }
            val isAllCompleted = CategoryCycleManager.allCategories.all { cat ->
                CategoryCycleManager.isCategoryCompleted(cat, updated)
            }
            CategoryCycleManager.CycleTransition(
                isRoundCompleted = isAllCompleted,
                newRound = if (isAllCompleted) currentRound + 1L else currentRound,
                updatedCompletedCategories = if (isAllCompleted) emptyList() else updated,
                previousRoundNumber = currentRound
            )
        }

        try {
            userRef.update(
                mapOf(
                    "categoryCycleRound" to cycleTransition.newRound,
                    "completedCategoriesInCycle" to cycleTransition.updatedCompletedCategories,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
        }

        return cycleTransition
    }

    /**
     * Permanently deletes user profile and data from Firestore for Google Play compliance.
     */
    suspend fun deleteUserData(userId: String) {
        try {
            db.collection("users").document(userId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId")
        }
    }
}
