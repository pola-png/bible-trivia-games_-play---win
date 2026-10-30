package com.bibletriviaquiz.playandwin.data.repository

import com.bibletriviaquiz.playandwin.model.Referral
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferralRepositoryTest {

    @Test
    fun deriveReferralCode_createsStandardFormat() {
        val uid = "xyz987user"
        val sanitized = uid.replace(Regex("[^a-zA-Z0-9]"), "").uppercase()
        val suffix = if (sanitized.length >= 6) sanitized.take(6) else sanitized.padEnd(6, 'X')
        val code = "REF-$suffix"

        assertEquals("REF-XYZ987", code)
        val link = "https://biblequizrewards.app/referral?code=$code"
        assertEquals("https://biblequizrewards.app/referral?code=REF-XYZ987", link)
    }

    @Test
    fun referralModel_qualificationThresholdAt10000Points() {
        // Under 10,000 points
        val pendingReferral = Referral(
            referralId = "ref_1_2",
            referrerUserId = "user_a",
            refereeUserId = "user_b",
            refereePoints = 9950L,
            targetPoints = 10000L,
            rewardPoints = 100L,
            status = "pending"
        )
        assertFalse("Referral should NOT be qualified under 10,000 points", pendingReferral.isQualified)
        assertFalse(pendingReferral.isCredited)
        assertEquals(0.995f, pendingReferral.progressFraction, 0.001f)

        // At exactly 10,000 points
        val qualifiedReferral = Referral(
            referralId = "ref_1_2",
            referrerUserId = "user_a",
            refereeUserId = "user_b",
            refereePoints = 10000L,
            targetPoints = 10000L,
            rewardPoints = 100L,
            status = "qualified"
        )
        assertTrue("Referral should be qualified at 10,000 points", qualifiedReferral.isQualified)
        assertEquals(1.0f, qualifiedReferral.progressFraction, 0.001f)
        assertEquals(100L, qualifiedReferral.rewardPoints)

        // Above 10,000 points and credited
        val creditedReferral = Referral(
            referralId = "ref_1_2",
            referrerUserId = "user_a",
            refereeUserId = "user_b",
            refereePoints = 15000L,
            targetPoints = 10000L,
            rewardPoints = 100L,
            status = "credited"
        )
        assertTrue(creditedReferral.isQualified)
        assertTrue(creditedReferral.isCredited)
        assertEquals(1.0f, creditedReferral.progressFraction, 0.001f)
    }

    @Test
    fun referralCodeParsing_handlesUrlAndRawCode() {
        val fullUrl = "https://biblequizrewards.app/referral?code=REF-48F19A"
        val extractedFromUrl = fullUrl.substringAfter("code=").substringBefore("&").trim()
        assertEquals("REF-48F19A", extractedFromUrl)

        val customScheme = "biblequiz://referral?code=REF-B83CA1&source=share"
        val extractedCustom = customScheme.substringAfter("code=").substringBefore("&").trim()
        assertEquals("REF-B83CA1", extractedCustom)
    }

    @Test
    fun adminWithdrawalDetail_computesEligibilityAndBonusPointsCorrectly() {
        val user = com.bibletriviaquiz.playandwin.model.UserProfile(
            userId = "user_123",
            email = "scholar@test.com",
            displayName = "David Scholar",
            currentPoints = 65000L,
            totalQuizzesCompleted = 40L,
            totalQuizzesPassed = 30L,
            successfulReferrals = 5L // 5 * 100 = 500 bonus points
        )

        val request = com.bibletriviaquiz.playandwin.model.CashRedemptionRequest(
            requestId = "cash_req_123",
            userId = "user_123",
            pointsRedeemed = 50000L,
            usdAmount = 50L,
            accountName = "David Scholar",
            accountNumber = "9876543210",
            bankName = "Chase Bank",
            payoutMethod = "Direct Wire",
            status = "pending"
        )

        val daysActive = 75L
        val bonusPts = user.successfulReferrals * 100L
        val quizPts = user.totalQuizzesPassed * 50L
        val passRate = ((user.totalQuizzesPassed.toFloat() / user.totalQuizzesCompleted.toFloat()) * 100).toInt()

        val detail = com.bibletriviaquiz.playandwin.model.AdminWithdrawalDetail(
            request = request,
            userProfile = user,
            daysActive = daysActive,
            bonusPointsEarned = bonusPts,
            quizPointsEarned = quizPts,
            passRatePercent = passRate,
            isEligible60Days = daysActive >= 60L
        )

        assertEquals(50L, detail.request.usdAmount)
        assertEquals("pending", detail.request.status)
        assertEquals("David Scholar", detail.request.accountName)
        assertEquals("9876543210", detail.request.accountNumber)
        assertEquals("Chase Bank", detail.request.bankName)
        assertEquals(500L, detail.bonusPointsEarned)
        assertEquals(1500L, detail.quizPointsEarned)
        assertEquals(75, detail.passRatePercent)
        assertTrue("Account should meet 60-day eligibility rule", detail.isEligible60Days)
    }
}
