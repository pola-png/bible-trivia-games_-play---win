package com.bibletriviaquiz.playandwin.data.repository

import com.bibletriviaquiz.playandwin.base.FirestoreEmulatorTestBase
import com.bibletriviaquiz.playandwin.model.QuizSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class QuizRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveCompletedQuizSession_passingScore_awards50Points(): Unit = runBlocking {
        val uniqueEmail = "alice_${System.nanoTime()}@test.com"
        signInTestUser(uniqueEmail)
        val repository = QuizRepository(firestore)

        repository.createOrUpdateUserProfile(uniqueEmail, "Alice")

        val session = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveCompletedQuizSession(
                category = "Old Testament",
                correctAnswers = 20,
                timedOutAnswers = 2
            )
        }

        assertTrue(session.isPassed)
        assertEquals(50L, session.pointsAwarded)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeUserProfile().first { it != null }
        }
        assertNotNull(profile)
        assertEquals(50L, profile!!.currentPoints)
        assertEquals(1L, profile.totalQuizzesPassed)
        assertTrue(profile.completedCategoriesInCycle.contains("Old Testament"))
    }

    @Test
    fun saveCompletedQuizSession_failingScore_awardsZeroPoints(): Unit = runBlocking {
        val uniqueEmail = "bob_${System.nanoTime()}@test.com"
        signInTestUser(uniqueEmail)
        val repository = QuizRepository(firestore)

        repository.createOrUpdateUserProfile(uniqueEmail, "Bob")

        val session = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveCompletedQuizSession(
                category = "New Testament",
                correctAnswers = 19,
                timedOutAnswers = 5
            )
        }

        assertTrue(!session.isPassed)
        assertEquals(0L, session.pointsAwarded)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeUserProfile().first { it != null }
        }
        assertNotNull(profile)
        assertEquals(0L, profile!!.currentPoints)
        assertEquals(0L, profile.totalQuizzesPassed)
        assertTrue(profile.completedCategoriesInCycle.isEmpty())
    }

    @Test
    fun observeUserSessions_unauthenticatedCaller_failsWithPermissionDenied(): Unit = runBlocking {
        auth.signOut()
        val repository = QuizRepository(firestore)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.observeUserSessions().first()
            }
            fail("Expected IllegalStateException for unauthenticated caller")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("User must be signed in") == true)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
    }
}
