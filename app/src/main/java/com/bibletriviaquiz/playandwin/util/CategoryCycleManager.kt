package com.bibletriviaquiz.playandwin.util

import android.content.Context
import android.content.SharedPreferences
import com.bibletriviaquiz.playandwin.data.repository.DefaultBibleQuestions
import java.util.Random

/**
 * Manages category round-robin cycles and per-user category permutations.
 *
 * Rules:
 * 1. Users cannot choose the same category twice consecutively.
 * 2. Users MUST choose and play through all 15 Bible categories once before repeating any category.
 * 3. Categories are permuted (shuffled) uniquely per user and per round, providing an engaging biblical journey.
 * 4. Once all 15 categories have been played, the round completes and advances to Round N+1 with a fresh permutation.
 */
object CategoryCycleManager {

    private const val PREFS_NAME = "bible_quiz_category_cycle_prefs"
    private const val KEY_ROUND_PREFIX = "cycle_round_"
    private const val KEY_COMPLETED_PREFIX = "completed_categories_"

    val allCategories: List<String>
        get() = DefaultBibleQuestions.categories

    val totalCategoryCount: Int
        get() = allCategories.size

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Generates a deterministic pseudo-random permutation of the 15 Bible categories
     * tailored to this specific user and round number.
     * The permutation remains stable throughout the round, but generates a brand new order in each round.
     */
    fun getPermutedCategories(userId: String, round: Long): List<String> {
        val safeUid = if (userId.isNotBlank()) userId else "guest_scholar"
        val seed = (safeUid.hashCode().toLong() shl 16) xor (round * 31L)
        val rng = Random(seed)
        return allCategories.shuffled(rng)
    }

    /**
     * Checks if a category has already been chosen/played in the given completed list.
     */
    fun isCategoryCompleted(category: String, completedCategories: Collection<String>): Boolean {
        val target = category.trim().lowercase()
        return completedCategories.any { it.trim().lowercase() == target }
    }

    /**
     * Returns remaining unplayed categories in their permuted order.
     */
    fun getRemainingCategories(permutedList: List<String>, completedCategories: Collection<String>): List<String> {
        return permutedList.filter { !isCategoryCompleted(it, completedCategories) }
    }

    data class CycleTransition(
        val isRoundCompleted: Boolean,
        val newRound: Long,
        val updatedCompletedCategories: List<String>,
        val previousRoundNumber: Long
    )

    /**
     * Reads locally cached round number for the user, falling back to 1.
     */
    fun getLocalRound(context: Context, userId: String, cloudRound: Long? = null): Long {
        val prefs = getPrefs(context)
        val localRound = prefs.getLong(KEY_ROUND_PREFIX + userId, 1L).coerceAtLeast(1L)
        val resolved = if (cloudRound != null && cloudRound > localRound) cloudRound else localRound
        if (resolved != localRound) {
            prefs.edit().putLong(KEY_ROUND_PREFIX + userId, resolved).apply()
        }
        return resolved
    }

    /**
     * Reads locally cached completed categories for the user, merging with cloud data if available.
     */
    fun getLocalCompletedCategories(
        context: Context,
        userId: String,
        cloudCompleted: List<String>? = null
    ): List<String> {
        val prefs = getPrefs(context)
        val localSet = prefs.getStringSet(KEY_COMPLETED_PREFIX + userId, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (!cloudCompleted.isNullOrEmpty()) {
            localSet.addAll(cloudCompleted)
            prefs.edit().putStringSet(KEY_COMPLETED_PREFIX + userId, localSet).apply()
        }
        return localSet.toList()
    }

    /**
     * Records that a category has been chosen / completed by the user.
     * If all 15 categories have now been completed in this round, completes the round,
     * advances to the next round, and resets completed categories so all 15 unlock again.
     */
    fun recordCategoryPlayed(
        context: Context,
        userId: String,
        category: String,
        currentRound: Long,
        currentCompleted: Collection<String>
    ): CycleTransition {
        val prefs = getPrefs(context)
        val updatedList = currentCompleted.toMutableList()

        if (!isCategoryCompleted(category, updatedList)) {
            updatedList.add(category.trim())
        }

        // Check if all categories in the pool have now been completed
        val isAllCompleted = allCategories.all { isCategoryCompleted(it, updatedList) } ||
                updatedList.size >= allCategories.size

        return if (isAllCompleted) {
            val nextRound = currentRound + 1L
            prefs.edit()
                .putLong(KEY_ROUND_PREFIX + userId, nextRound)
                .putStringSet(KEY_COMPLETED_PREFIX + userId, emptySet())
                .apply()

            CycleTransition(
                isRoundCompleted = true,
                newRound = nextRound,
                updatedCompletedCategories = emptyList(),
                previousRoundNumber = currentRound
            )
        } else {
            prefs.edit()
                .putLong(KEY_ROUND_PREFIX + userId, currentRound)
                .putStringSet(KEY_COMPLETED_PREFIX + userId, updatedList.toSet())
                .apply()

            CycleTransition(
                isRoundCompleted = false,
                newRound = currentRound,
                updatedCompletedCategories = updatedList,
                previousRoundNumber = currentRound
            )
        }
    }
}
