package com.bibletriviaquiz.playandwin.model

data class AdminWithdrawalDetail(
    val request: CashRedemptionRequest,
    val userProfile: UserProfile?,
    val daysActive: Long,
    val bonusPointsEarned: Long,
    val quizPointsEarned: Long,
    val passRatePercent: Int,
    val isEligible60Days: Boolean
)
