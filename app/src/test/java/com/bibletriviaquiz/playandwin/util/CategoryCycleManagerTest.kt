package com.bibletriviaquiz.playandwin.util

import com.bibletriviaquiz.playandwin.data.repository.DefaultBibleQuestions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryCycleManagerTest {

    private val allCategories = DefaultBibleQuestions.categories

    @Test
    fun getPermutedCategories_containsAll15CategoriesWithoutDuplicates() {
        val permuted = CategoryCycleManager.getPermutedCategories("user_123", round = 1L)

        assertEquals(15, permuted.size)
        assertEquals(allCategories.toSet(), permuted.toSet())
    }

    @Test
    fun getPermutedCategories_isDeterministicForSameUserAndRound() {
        val run1 = CategoryCycleManager.getPermutedCategories("scholar_xyz", round = 1L)
        val run2 = CategoryCycleManager.getPermutedCategories("scholar_xyz", round = 1L)

        assertEquals(run1, run2)
    }

    @Test
    fun getPermutedCategories_changesOrderBetweenDifferentRounds() {
        val round1 = CategoryCycleManager.getPermutedCategories("scholar_xyz", round = 1L)
        val round2 = CategoryCycleManager.getPermutedCategories("scholar_xyz", round = 2L)

        // Same items, but different permuted order
        assertEquals(round1.toSet(), round2.toSet())
        assertNotEquals(round1, round2)
    }

    @Test
    fun getPermutedCategories_changesOrderBetweenDifferentUsers() {
        val userA = CategoryCycleManager.getPermutedCategories("user_alice_123", round = 1L)
        val userB = CategoryCycleManager.getPermutedCategories("user_bob_456", round = 1L)

        assertEquals(userA.toSet(), userB.toSet())
        assertNotEquals(userA, userB)
    }

    @Test
    fun isCategoryCompleted_detectsPlayedCategoryCaseInsensitively() {
        val completed = listOf("Old Testament", "Miracles of Jesus")

        assertTrue(CategoryCycleManager.isCategoryCompleted("Old Testament", completed))
        assertTrue(CategoryCycleManager.isCategoryCompleted("old testament", completed))
        assertTrue(CategoryCycleManager.isCategoryCompleted("MIRACLES OF JESUS", completed))
        assertFalse(CategoryCycleManager.isCategoryCompleted("Women in the Bible", completed))
    }

    @Test
    fun getRemainingCategories_filtersOutCompletedCategories() {
        val permuted = CategoryCycleManager.getPermutedCategories("test_user", round = 1L)
        val completed = listOf(permuted[0], permuted[1], permuted[2])

        val remaining = CategoryCycleManager.getRemainingCategories(permuted, completed)

        assertEquals(12, remaining.size)
        assertFalse(remaining.contains(permuted[0]))
        assertFalse(remaining.contains(permuted[1]))
        assertFalse(remaining.contains(permuted[2]))
    }
}
