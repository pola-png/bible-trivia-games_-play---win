package com.bibletriviaquiz.playandwin.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bibletriviaquiz.playandwin.model.Question
import java.util.concurrent.TimeUnit

/**
 * Manages question serving history to guarantee that questions do NOT repeat
 * for at least 10 days for any user answering Bible quiz questions.
 * Tracks both Question IDs and normalized Question Texts to eliminate text duplicates.
 */
object QuestionHistoryManager {

    private const val TAG = "QuestionHistoryManager"
    private const val PREFS_PREFIX = "bible_quiz_history_"
    private const val TEXT_PREFS_PREFIX = "bible_quiz_text_history_"
    const val DEFAULT_NON_REPEAT_DAYS = 10
    val NON_REPEAT_WINDOW_MS: Long = TimeUnit.DAYS.toMillis(DEFAULT_NON_REPEAT_DAYS.toLong()) // 10 days in ms

    fun normalizeText(text: String): String {
        return text.lowercase().replace(Regex("[^a-z0-9]"), "").trim()
    }

    /**
     * Returns the set of question IDs that were answered or served to this user
     * within the non-repeat window (default 10 days).
     */
    fun getRecentlyServedQuestionIds(
        context: Context,
        userId: String,
        windowMillis: Long = NON_REPEAT_WINDOW_MS
    ): Set<String> {
        val safeUid = sanitizeUserId(userId)
        val prefs = getPrefs(context, safeUid)
        val cutoff = System.currentTimeMillis() - windowMillis
        val allEntries = prefs.all
        val recentIds = HashSet<String>(allEntries.size)
        val staleKeys = mutableListOf<String>()

        for ((key, value) in allEntries) {
            val timestamp = when (value) {
                is Long -> value
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: 0L
                else -> 0L
            }
            if (timestamp >= cutoff) {
                recentIds.add(key)
            } else if (System.currentTimeMillis() - timestamp > TimeUnit.DAYS.toMillis(30)) {
                staleKeys.add(key)
            }
        }

        if (staleKeys.isNotEmpty()) {
            val editor = prefs.edit()
            staleKeys.forEach { editor.remove(it) }
            editor.apply()
        }

        return recentIds
    }

    /**
     * Returns the set of normalized question texts answered by this user
     * within the non-repeat window (default 10 days).
     */
    fun getRecentlyServedQuestionTexts(
        context: Context,
        userId: String,
        windowMillis: Long = NON_REPEAT_WINDOW_MS
    ): Set<String> {
        val safeUid = sanitizeUserId(userId)
        val prefs = getTextPrefs(context, safeUid)
        val cutoff = System.currentTimeMillis() - windowMillis
        val allEntries = prefs.all
        val recentTexts = HashSet<String>(allEntries.size)
        val staleKeys = mutableListOf<String>()

        for ((key, value) in allEntries) {
            val timestamp = when (value) {
                is Long -> value
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: 0L
                else -> 0L
            }
            if (timestamp >= cutoff) {
                recentTexts.add(key)
            } else if (System.currentTimeMillis() - timestamp > TimeUnit.DAYS.toMillis(30)) {
                staleKeys.add(key)
            }
        }

        if (staleKeys.isNotEmpty()) {
            val editor = prefs.edit()
            staleKeys.forEach { editor.remove(it) }
            editor.apply()
        }

        return recentTexts
    }

    /**
     * Records questions that were served or answered by the user, associating them
     * with the current timestamp so they cannot be served again for at least 10 days.
     */
    fun recordQuestionsServed(
        context: Context,
        userId: String,
        questionIds: List<String>,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (questionIds.isEmpty()) return
        val safeUid = sanitizeUserId(userId)
        val prefs = getPrefs(context, safeUid)
        val editor = prefs.edit()
        for (qId in questionIds) {
            if (qId.isNotBlank()) {
                editor.putLong(qId.trim(), timestamp)
            }
        }
        editor.apply()
    }

    /**
     * Records full Question objects, capturing both IDs and normalized text hashes
     * to guarantee zero text repetition within the 10-day window.
     */
    fun recordFullQuestionsServed(
        context: Context,
        userId: String,
        questions: List<Question>,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (questions.isEmpty()) return
        val safeUid = sanitizeUserId(userId)
        val idPrefs = getPrefs(context, safeUid)
        val textPrefs = getTextPrefs(context, safeUid)
        val idEditor = idPrefs.edit()
        val textEditor = textPrefs.edit()

        for (q in questions) {
            if (q.questionId.isNotBlank()) {
                idEditor.putLong(q.questionId.trim(), timestamp)
            }
            val norm = normalizeText(q.questionText)
            if (norm.isNotBlank()) {
                textEditor.putLong(norm, timestamp)
            }
        }
        idEditor.apply()
        textEditor.apply()
        Log.d(TAG, "Recorded ${questions.size} full questions for user $safeUid")
    }

    /**
     * Checks if a specific question is eligible to be served (i.e. not served in the last 10 days).
     */
    fun isQuestionEligible(
        context: Context,
        userId: String,
        questionId: String,
        questionText: String? = null,
        windowMillis: Long = NON_REPEAT_WINDOW_MS
    ): Boolean {
        val safeUid = sanitizeUserId(userId)
        val prefs = getPrefs(context, safeUid)
        val lastServed = prefs.getLong(questionId.trim(), 0L)
        if (lastServed != 0L) {
            val elapsed = System.currentTimeMillis() - lastServed
            if (elapsed < windowMillis) return false
        }

        if (!questionText.isNullOrBlank()) {
            val textPrefs = getTextPrefs(context, safeUid)
            val norm = normalizeText(questionText)
            val lastTextServed = textPrefs.getLong(norm, 0L)
            if (lastTextServed != 0L) {
                val elapsed = System.currentTimeMillis() - lastTextServed
                if (elapsed < windowMillis) return false
            }
        }

        return true
    }

    /**
     * Returns statistics on questions answered in the last 10 days.
     */
    fun getHistorySummary(
        context: Context,
        userId: String,
        windowMillis: Long = NON_REPEAT_WINDOW_MS
    ): HistorySummary {
        val safeUid = sanitizeUserId(userId)
        val prefs = getPrefs(context, safeUid)
        val cutoff = System.currentTimeMillis() - windowMillis
        var countInWindow = 0
        var totalEver = 0

        for ((_, value) in prefs.all) {
            totalEver++
            val timestamp = when (value) {
                is Long -> value
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: 0L
                else -> 0L
            }
            if (timestamp >= cutoff) {
                countInWindow++
            }
        }

        return HistorySummary(
            totalAnsweredInLast10Days = countInWindow,
            totalHistoricalRecorded = totalEver,
            nonRepeatWindowDays = (windowMillis / TimeUnit.DAYS.toMillis(1)).toInt()
        )
    }

    /**
     * Clears history for a user (useful for testing or profile reset).
     */
    fun clearHistory(context: Context, userId: String) {
        val safeUid = sanitizeUserId(userId)
        getPrefs(context, safeUid).edit().clear().apply()
        getTextPrefs(context, safeUid).edit().clear().apply()
    }

    private fun getPrefs(context: Context, safeUid: String): SharedPreferences {
        return context.applicationContext.getSharedPreferences(
            "$PREFS_PREFIX$safeUid",
            Context.MODE_PRIVATE
        )
    }

    private fun getTextPrefs(context: Context, safeUid: String): SharedPreferences {
        return context.applicationContext.getSharedPreferences(
            "$TEXT_PREFS_PREFIX$safeUid",
            Context.MODE_PRIVATE
        )
    }

    private fun sanitizeUserId(userId: String): String {
        return if (userId.isBlank()) "guest" else userId.replace(Regex("[^a-zA-Z0-9_]"), "_")
    }

    data class HistorySummary(
        val totalAnsweredInLast10Days: Int,
        val totalHistoricalRecorded: Int,
        val nonRepeatWindowDays: Int
    )
}
