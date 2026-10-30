package com.bibletriviaquiz.playandwin.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuestionHistoryManagerTest {

    private lateinit var context: Context
    private val testUserId = "user_scholar_test_101"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        QuestionHistoryManager.clearHistory(context, testUserId)
    }

    @Test
    fun getRecentlyServedQuestionIds_returnsQuestionsRecordedWithin10Days() {
        val now = System.currentTimeMillis()
        val recentQuestions = listOf("q_ot_001", "q_ot_002", "q_ot_003")

        QuestionHistoryManager.recordQuestionsServed(context, testUserId, recentQuestions, timestamp = now)

        val served = QuestionHistoryManager.getRecentlyServedQuestionIds(context, testUserId)

        assertEquals(3, served.size)
        assertTrue(served.contains("q_ot_001"))
        assertTrue(served.contains("q_ot_002"))
        assertTrue(served.contains("q_ot_003"))
        assertFalse(QuestionHistoryManager.isQuestionEligible(context, testUserId, "q_ot_001"))
    }

    @Test
    fun getRecentlyServedQuestionIds_excludesQuestionsOlderThan10Days() {
        val now = System.currentTimeMillis()
        val elevenDaysAgo = now - TimeUnit.DAYS.toMillis(11)

        val oldQuestions = listOf("q_old_001", "q_old_002")
        QuestionHistoryManager.recordQuestionsServed(context, testUserId, oldQuestions, timestamp = elevenDaysAgo)

        val recentQuestions = listOf("q_fresh_001")
        QuestionHistoryManager.recordQuestionsServed(context, testUserId, recentQuestions, timestamp = now)

        val servedWithin10Days = QuestionHistoryManager.getRecentlyServedQuestionIds(context, testUserId)

        assertEquals(1, servedWithin10Days.size)
        assertTrue(servedWithin10Days.contains("q_fresh_001"))
        assertFalse(servedWithin10Days.contains("q_old_001"))
        assertFalse(servedWithin10Days.contains("q_old_002"))

        // Old questions are eligible again after 10 days
        assertTrue(QuestionHistoryManager.isQuestionEligible(context, testUserId, "q_old_001"))
        assertFalse(QuestionHistoryManager.isQuestionEligible(context, testUserId, "q_fresh_001"))
    }

    @Test
    fun getHistorySummary_accuratelyCounts10DayWindowVersusTotal() {
        val now = System.currentTimeMillis()
        val twelveDaysAgo = now - TimeUnit.DAYS.toMillis(12)

        QuestionHistoryManager.recordQuestionsServed(context, testUserId, listOf("q1", "q2"), timestamp = twelveDaysAgo)
        QuestionHistoryManager.recordQuestionsServed(context, testUserId, listOf("q3", "q4", "q5"), timestamp = now)

        val summary = QuestionHistoryManager.getHistorySummary(context, testUserId)

        assertEquals(3, summary.totalAnsweredInLast10Days)
        assertEquals(5, summary.totalHistoricalRecorded)
        assertEquals(10, summary.nonRepeatWindowDays)
    }

    @Test
    fun recordFullQuestionsServed_recordsBothIdsAndNormalizedTexts() {
        val now = System.currentTimeMillis()
        val questions = listOf(
            com.bibletriviaquiz.playandwin.model.Question(
                questionId = "q_test_101",
                questionText = "Where was Jesus born in Bethlehem?",
                category = "Life and Teachings of Jesus"
            ),
            com.bibletriviaquiz.playandwin.model.Question(
                questionId = "q_test_102",
                questionText = "Who built the great wooden ark?",
                category = "Old Testament"
            )
        )

        QuestionHistoryManager.recordFullQuestionsServed(context, testUserId, questions, timestamp = now)

        val servedIds = QuestionHistoryManager.getRecentlyServedQuestionIds(context, testUserId)
        val servedTexts = QuestionHistoryManager.getRecentlyServedQuestionTexts(context, testUserId)

        assertEquals(2, servedIds.size)
        assertTrue(servedIds.contains("q_test_101"))
        assertTrue(servedIds.contains("q_test_102"))

        assertEquals(2, servedTexts.size)
        assertTrue(servedTexts.contains("wherewasjesusborninbethlehem"))
        assertTrue(servedTexts.contains("whobuiltthegreatwoodenark"))

        assertFalse(QuestionHistoryManager.isQuestionEligible(context, testUserId, "any_id", "Where was Jesus born in Bethlehem?"))
    }
}
