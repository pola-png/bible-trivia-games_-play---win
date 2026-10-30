package com.bibletriviaquiz.playandwin.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProceduralBibleQuestionBankTest {

    @Test
    fun questionBank_hasAtLeast100000QuestionsForEachCategory() {
        assertEquals(100_000, ProceduralBibleQuestionBank.QUESTIONS_PER_CATEGORY)

        for (category in DefaultBibleQuestions.categories) {
            val count = ProceduralBibleQuestionBank.getQuestionCountForCategory(category)
            assertTrue("Category '$category' must have at least 100,000 questions", count >= 100_000)
        }
    }

    @Test
    fun getQuestion_synthesizesWellFormedBibleQuestionsAcrossCategories() {
        for (category in DefaultBibleQuestions.categories) {
            val sampleIndices = listOf(0, 1492, 25000, 77777, 99999)
            for (idx in sampleIndices) {
                val q = ProceduralBibleQuestionBank.getQuestion(category, idx)

                assertNotNull(q)
                assertTrue("Question ID must not be blank", q.questionId.isNotBlank())
                assertTrue("Question ID must include bank prefix", q.questionId.startsWith("q_bank_"))
                assertTrue("Question text must be substantive", q.questionText.length > 10)
                assertEquals("Question must have exactly 4 options", 4, q.options.size)
                assertTrue("Options must be unique", q.options.distinct().size == 4)
                assertTrue("Correct option index must be 0..3", q.correctOptionIndex in 0..3)
                assertNotNull("Bible reference must exist", q.bibleReference)
                assertTrue("Bible reference must not be empty", q.bibleReference!!.isNotBlank())
                assertNotNull("Explanation must exist", q.explanation)
                assertTrue("Explanation must not be empty", q.explanation!!.isNotBlank())
            }
        }
    }

    @Test
    fun getQuestionsForCategory_strictlyExcludesRecentlyServedQuestionIds() {
        val category = "Old Testament"
        // Generate an exclusion set simulating questions answered within the 10-day window
        val excluded = (0 until 30).map { idx ->
            ProceduralBibleQuestionBank.getQuestion(category, idx).questionId
        }.toSet()

        val selected = ProceduralBibleQuestionBank.getQuestionsForCategory(
            category = category,
            count = 30,
            excludedIds = excluded,
            seed = 42L
        )

        assertEquals(30, selected.size)
        // Verify none of the 30 selected questions are in the excluded 10-day window set
        for (q in selected) {
            assertFalse(
                "Question ${q.questionId} was served in the 10-day window and must NOT be repeated!",
                excluded.contains(q.questionId)
            )
        }
        // Verify all 30 returned questions are distinct
        assertEquals(30, selected.map { it.questionId }.distinct().size)
    }

    @Test
    fun getQuestionsForCategory_preservesCustomQuestionsWhenNotExcluded() {
        val category = "Miracles of Jesus"
        val customQ = ProceduralBibleQuestionBank.getQuestion(category, 10).copy(
            questionId = "custom_miracle_999",
            questionText = "What was the first miracle of Jesus at Cana?"
        )

        val selected = ProceduralBibleQuestionBank.getQuestionsForCategory(
            category = category,
            count = 30,
            excludedIds = emptySet(),
            customQuestions = listOf(customQ),
            seed = 123L
        )

        assertEquals(30, selected.size)
        assertTrue("Custom question should be included when not excluded", selected.any { it.questionId == "custom_miracle_999" })
    }

    @Test
    fun getQuestionsForCategory_guaranteesAll30QuestionsHaveUniqueTextsAcrossAll15Categories() {
        for (category in DefaultBibleQuestions.categories) {
            val questions = ProceduralBibleQuestionBank.getQuestionsForCategory(
                category = category,
                count = 30,
                seed = 777L
            )

            assertEquals("Category '$category' must return exactly 30 questions", 30, questions.size)

            val uniqueTexts = questions.map { it.questionText.lowercase().replace(Regex("[^a-z0-9]"), "") }.toSet()
            assertEquals("Category '$category' must NOT have duplicate question texts in a 30-question session", 30, uniqueTexts.size)
        }
    }

    @Test
    fun getQuestionsForCategory_strictlyExcludesRecentlyAnsweredQuestionTexts() {
        val category = "Life and Teachings of Jesus"
        val batch1 = ProceduralBibleQuestionBank.getQuestionsForCategory(category, count = 15, seed = 100L)
        val answeredTexts = batch1.map { it.questionText.lowercase().replace(Regex("[^a-z0-9]"), "") }.toSet()

        val batch2 = ProceduralBibleQuestionBank.getQuestionsForCategory(
            category = category,
            count = 15,
            excludedTexts = answeredTexts,
            seed = 200L
        )

        for (q in batch2) {
            val norm = q.questionText.lowercase().replace(Regex("[^a-z0-9]"), "")
            assertFalse("Batch 2 question '${q.questionText}' was in excluded 10-day window and should not repeat", answeredTexts.contains(norm))
        }
    }
}
