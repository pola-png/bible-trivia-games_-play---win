package com.bibletriviaquiz.playandwin.data.repository

import android.content.Context
import android.util.Log
import com.bibletriviaquiz.playandwin.R
import com.bibletriviaquiz.playandwin.model.PointTransaction
import com.bibletriviaquiz.playandwin.model.Referral
import com.bibletriviaquiz.playandwin.model.ReferralCodeLookup
import com.bibletriviaquiz.playandwin.model.UserProfile
import com.bibletriviaquiz.playandwin.util.OperationType
import com.bibletriviaquiz.playandwin.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ReferralRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing referral system.")
    }

    /**
     * Derives a deterministic, uppercase referral code based on the user's UID.
     * Example: REF-7C3F8A
     */
    fun deriveReferralCode(userId: String = requireUserId()): String {
        val sanitized = userId.replace(Regex("[^a-zA-Z0-9]"), "").uppercase()
        val suffix = if (sanitized.length >= 6) sanitized.take(6) else sanitized.padEnd(6, 'X')
        return "REF-$suffix"
    }

    /**
     * Builds the personalized referral link for sharing.
     */
    fun buildReferralLink(code: String): String {
        return "https://biblequizrewards.app/referral?code=$code"
    }

    /**
     * Ensures the current user's referral code is registered in Firestore
     * under `referralCodes/{code}` and `users/{userId}.referralCode`.
     */
    suspend fun ensureReferralCodeRegistered(displayName: String): String {
        val uid = requireUserId()
        val code = deriveReferralCode(uid)

        try {
            val codeRef = db.collection("referralCodes").document(code)
            val codeSnapshot = codeRef.get().await()

            if (!codeSnapshot.exists()) {
                val payload = hashMapOf(
                    "code" to code,
                    "userId" to uid,
                    "displayName" to displayName,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                codeRef.set(payload, SetOptions.merge()).await()
            }

            // Sync with UserProfile
            val userRef = db.collection("users").document(uid)
            userRef.set(
                mapOf(
                    "referralCode" to code,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            return code
        } catch (e: Exception) {
            Log.e("ReferralRepo", "Failed to register referral code: ${e.message}", e)
            return code
        }
    }

    /**
     * Applies a friend's referral code to the current user's account.
     * Validates that:
     * 1. Code is not empty.
     * 2. Cannot refer self.
     * 3. Cannot apply a referral code if already referred.
     * 4. Code exists in referralCodes lookup.
     */
    suspend fun applyReferralCode(rawCode: String): Result<String> {
        val uid = requireUserId()
        val cleanCode = rawCode.trim().uppercase()

        if (cleanCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter a valid referral code."))
        }

        val myCode = deriveReferralCode(uid)
        if (cleanCode == myCode) {
            return Result.failure(IllegalArgumentException("You cannot use your own referral code."))
        }

        try {
            // Check if current user already has a referrer
            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            val existingReferrer = userSnap.getString("referredByUserId")
            if (!existingReferrer.isNullOrBlank()) {
                return Result.failure(IllegalStateException("You have already used a referral code."))
            }

            // Look up the code in referralCodes
            val lookupRef = db.collection("referralCodes").document(cleanCode)
            val lookupSnap = lookupRef.get().await()
            if (!lookupSnap.exists()) {
                return Result.failure(IllegalArgumentException("Referral code '$cleanCode' was not found. Please verify the code."))
            }

            val referrerUid = lookupSnap.getString("userId") ?: return Result.failure(IllegalStateException("Invalid referral data."))
            val referrerName = lookupSnap.getString("displayName") ?: "Bible Scholar"

            if (referrerUid == uid) {
                return Result.failure(IllegalArgumentException("You cannot refer yourself."))
            }

            val currentPoints = userSnap.getLong("currentPoints") ?: 0L
            val referralId = "ref_${referrerUid}_${uid}"
            val referralRef = db.collection("referrals").document(referralId)

            val referralPayload = hashMapOf(
                "referralId" to referralId,
                "referrerUserId" to referrerUid,
                "referrerCode" to cleanCode,
                "refereeUserId" to uid,
                "refereeDisplayName" to (auth.currentUser?.displayName ?: "New Scholar"),
                "refereeEmail" to (auth.currentUser?.email ?: ""),
                "refereePoints" to currentPoints,
                "targetPoints" to 10000L,
                "rewardPoints" to 100L,
                "status" to if (currentPoints >= 10000L) "qualified" else "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            referralRef.set(referralPayload).await()

            // Update referee's user profile
            userRef.set(
                mapOf(
                    "referredByCode" to cleanCode,
                    "referredByUserId" to referrerUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            return Result.success("Referral applied! Referred by $referrerName. Once you reach 10,000 points, $referrerName will receive 100 points!")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "referrals")
            return Result.failure(e)
        }
    }

    /**
     * Observes all friends referred by the current user.
     */
    fun observeMyReferrals(): Flow<List<Referral>> = flow {
        val uid = requireUserId()
        val query = db.collection("referrals").whereEqualTo("referrerUserId", uid)
        query.snapshots().map { snapshot ->
            snapshot.toObjects(Referral::class.java).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "referrals")
            throw e
        }.collect { emit(it) }
    }

    /**
     * Observes current user's referral record if they were referred by a friend.
     */
    fun observeMyReferralAsReferee(): Flow<Referral?> = flow {
        val uid = requireUserId()
        val query = db.collection("referrals").whereEqualTo("refereeUserId", uid)
        query.snapshots().map { snapshot ->
            snapshot.toObjects(Referral::class.java).firstOrNull()
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "referrals")
            throw e
        }.collect { emit(it) }
    }

    /**
     * Synchronizes referee points whenever the referee gains points in the app.
     */
    suspend fun syncRefereePoints(points: Long) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val query = db.collection("referrals")
                .whereEqualTo("refereeUserId", uid)
                .get()
                .await()

            for (doc in query.documents) {
                val currentStatus = doc.getString("status") ?: "pending"
                val newStatus = if (currentStatus != "credited" && points >= 10000L) "qualified" else currentStatus
                doc.reference.update(
                    mapOf(
                        "refereePoints" to points,
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            Log.w("ReferralRepo", "syncRefereePoints error: ${e.message}")
        }
    }

    /**
     * Scans all referrals where the current user is the referrer.
     * If any referee has earned >= 10,000 points and the reward hasn't been credited yet:
     * 1. Updates referral doc status to "credited".
     * 2. Credits +100 points to current user's wallet.
     * 3. Creates a "referral_bonus" PointTransaction in the append-only ledger.
     * 4. Updates successfulReferrals count.
     */
    suspend fun checkAndClaimReferralBonuses(): Result<ClaimResult> {
        val uid = requireUserId()
        try {
            val query = db.collection("referrals")
                .whereEqualTo("referrerUserId", uid)
                .get()
                .await()

            var totalPointsAwarded = 0L
            var creditedCount = 0
            val creditedFriends = mutableListOf<String>()

            for (doc in query.documents) {
                val ref = doc.toObject(Referral::class.java) ?: continue
                if (ref.status != "credited" && ref.refereePoints >= 10000L) {
                    val reward = if (ref.rewardPoints > 0L) ref.rewardPoints else 100L

                    // Mark as credited
                    doc.reference.update(
                        mapOf(
                            "status" to "credited",
                            "creditedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()

                    // Credit user's wallet
                    val userRef = db.collection("users").document(uid)
                    userRef.update(
                        mapOf(
                            "currentPoints" to FieldValue.increment(reward),
                            "successfulReferrals" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()

                    // Record PointTransaction
                    val txId = "tx_ref_${UUID.randomUUID().toString().replace("-", "").take(14)}"
                    val txRef = db.collection("pointTransactions").document(txId)
                    val txPayload = hashMapOf(
                        "transactionId" to txId,
                        "userId" to uid,
                        "type" to "referral_bonus",
                        "amount" to reward,
                        "description" to "Referral Reward: Friend ${ref.refereeDisplayName} earned 10,000+ points!",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    txRef.set(txPayload).await()

                    totalPointsAwarded += reward
                    creditedCount++
                    creditedFriends.add(ref.refereeDisplayName)
                }
            }

            return Result.success(
                ClaimResult(
                    claimedCount = creditedCount,
                    pointsAwarded = totalPointsAwarded,
                    friendNames = creditedFriends
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "referrals")
            return Result.failure(e)
        }
    }

    /**
     * Testing / Verification helper:
     * Simulates a referee earning points (e.g. setting refereePoints to 10,000)
     * so that the user or evaluator can test and observe the 100-point reward flow.
     */
    suspend fun simulateRefereePointsForTesting(referralId: String, targetPoints: Long): Result<Unit> {
        try {
            val docRef = db.collection("referrals").document(referralId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalArgumentException("Referral record not found."))
            }
            val currentStatus = snap.getString("status") ?: "pending"
            val newStatus = if (currentStatus != "credited" && targetPoints >= 10000L) "qualified" else currentStatus

            docRef.update(
                mapOf(
                    "refereePoints" to targetPoints,
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}

data class ClaimResult(
    val claimedCount: Int,
    val pointsAwarded: Long,
    val friendNames: List<String>
)
