package com.bibletriviaquiz.playandwin.ui.screens

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdUnits
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import com.bibletriviaquiz.playandwin.ads.BannerAdView
import com.bibletriviaquiz.playandwin.ads.NativeAdCard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibletriviaquiz.playandwin.audio.BuzzerManager
import com.bibletriviaquiz.playandwin.data.repository.ProceduralBibleQuestionBank
import com.bibletriviaquiz.playandwin.model.Question
import com.bibletriviaquiz.playandwin.ui.theme.EmeraldGreen
import com.bibletriviaquiz.playandwin.ui.theme.GoldenBronze
import com.bibletriviaquiz.playandwin.ui.theme.QuizRed
import com.bibletriviaquiz.playandwin.ui.theme.QuizRedDark
import com.bibletriviaquiz.playandwin.ui.theme.RoyalBlue
import com.bibletriviaquiz.playandwin.ui.theme.RoyalNavy
import com.bibletriviaquiz.playandwin.ui.theme.WarmGold
import com.bibletriviaquiz.playandwin.util.QuestionHistoryManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class QuestionState {
    READY,
    COUNTING_DOWN,
    ANSWER_SUBMITTED,
    TIMEOUT,
    FEEDBACK,
    AD_CHECKPOINT,
    COMPLETED
}

private data class OptionTheme(
    val letter: String,
    val badgeBg: Color,
    val cardBg: Color,
    val borderColor: Color,
    val accentColor: Color
)

private val OptionThemes = listOf(
    OptionTheme(
        letter = "A",
        badgeBg = Color(0xFF1D4ED8), // Vibrant Royal Blue
        cardBg = Color(0xFFF0F7FF),  // Soft Bright Sky
        borderColor = Color(0xFF3B82F6), // Vibrant Blue 2.5dp
        accentColor = Color(0xFF1D4ED8)
    ),
    OptionTheme(
        letter = "B",
        badgeBg = Color(0xFFD97706), // Vibrant Amber Gold
        cardBg = Color(0xFFFFFBEB),  // Soft Warm Cream
        borderColor = Color(0xFFF59E0B), // Vibrant Gold 2.5dp
        accentColor = Color(0xFFB45309)
    ),
    OptionTheme(
        letter = "C",
        badgeBg = Color(0xFF047857), // Vibrant Emerald Green
        cardBg = Color(0xFFF0FDF4),  // Soft Mint
        borderColor = Color(0xFF10B981), // Vibrant Green 2.5dp
        accentColor = Color(0xFF047857)
    ),
    OptionTheme(
        letter = "D",
        badgeBg = Color(0xFF7C3AED), // Vibrant Royal Purple
        cardBg = Color(0xFFFAF5FF),  // Soft Bright Lavender
        borderColor = Color(0xFF8B5CF6), // Vibrant Violet 2.5dp
        accentColor = Color(0xFF6D28D9)
    )
)

private data class OptionStyle(
    val cardBg: Color,
    val cardBorder: Color,
    val letterBg: Color,
    val letterText: Color,
    val textColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    category: String,
    questions: List<Question>,
    onQuizComplete: (correctCount: Int, timedOutCount: Int) -> Unit,
    onExitQuiz: () -> Unit,
    onShowRewardedAd: ((onCompleted: (Boolean) -> Unit) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = Firebase.auth
    val uid = auth.currentUser?.uid ?: "guest"

    // Exactly 30 questions required with 10-day non-repeat guarantee from the 100,000-question bank
    val sessionQuestions = remember(category) {
        val excludedIds = QuestionHistoryManager.getRecentlyServedQuestionIds(context, uid)
        val excludedTexts = QuestionHistoryManager.getRecentlyServedQuestionTexts(context, uid)
        val selected = ProceduralBibleQuestionBank.getQuestionsForCategory(
            category = category,
            count = 30,
            excludedIds = excludedIds,
            excludedTexts = excludedTexts,
            customQuestions = questions
        )
        // Record these 30 questions with timestamp so neither ID nor text can be repeated for at least 10 days
        QuestionHistoryManager.recordFullQuestionsServed(
            context = context,
            userId = uid,
            questions = selected
        )
        selected
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var timedOutCount by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var questionState by remember { mutableStateOf(QuestionState.COUNTING_DOWN) }

    // Authoritative 15-second monotonic countdown timer
    val timerDurationMs = 15000L
    var deadlineMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime() + timerDurationMs) }
    var remainingMillis by remember { mutableLongStateOf(timerDurationMs) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var showAdCheckpoint by remember { mutableStateOf(false) }
    var checkpointRewardedAdsWatched by remember { mutableIntStateOf(0) }
    var isAdLoadingOrShowing by remember { mutableStateOf(false) }
    var adErrorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val currentQuestion = sessionQuestions.getOrNull(currentIndex)

    // Handle system back button
    BackHandler {
        showQuitDialog = true
    }

    // Active 3-second timer loop using monotonic SystemClock
    LaunchedEffect(currentIndex, questionState) {
        if (questionState == QuestionState.COUNTING_DOWN) {
            deadlineMs = SystemClock.elapsedRealtime() + timerDurationMs
            while (questionState == QuestionState.COUNTING_DOWN) {
                val now = SystemClock.elapsedRealtime()
                val left = (deadlineMs - now).coerceAtLeast(0L)
                remainingMillis = left

                if (left <= 0L) {
                    // ATOMIC TIMEOUT TRIGGER
                    questionState = QuestionState.TIMEOUT
                    timedOutCount++
                    // Play unmistakable buzzer sound once
                    BuzzerManager.playBuzzer()
                    break
                }
                delay(25L)
            }
        }
    }

    // Post-Timeout & Feedback Progression Handler
    LaunchedEffect(questionState) {
        if (questionState == QuestionState.TIMEOUT) {
            // Display timeout buzzer graphic ("XXX") for ~900ms
            delay(900L)
            handleAdvanceAfterQuestion(
                currentIndex = currentIndex,
                totalCount = sessionQuestions.size,
                onShowAd = {
                    checkpointRewardedAdsWatched = 0
                    isAdLoadingOrShowing = false
                    adErrorMessage = null
                    showAdCheckpoint = true
                },
                onNextQuestion = {
                    currentIndex++
                    selectedOptionIndex = null
                    questionState = QuestionState.COUNTING_DOWN
                },
                onComplete = {
                    questionState = QuestionState.COMPLETED
                    onQuizComplete(correctCount, timedOutCount)
                }
            )
        } else if (questionState == QuestionState.FEEDBACK) {
            // Brief 700ms feedback for answered question
            delay(700L)
            handleAdvanceAfterQuestion(
                currentIndex = currentIndex,
                totalCount = sessionQuestions.size,
                onShowAd = {
                    checkpointRewardedAdsWatched = 0
                    isAdLoadingOrShowing = false
                    adErrorMessage = null
                    showAdCheckpoint = true
                },
                onNextQuestion = {
                    currentIndex++
                    selectedOptionIndex = null
                    questionState = QuestionState.COUNTING_DOWN
                },
                onComplete = {
                    questionState = QuestionState.COMPLETED
                    onQuizComplete(correctCount, timedOutCount)
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Question ${currentIndex + 1} of 30",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { showQuitDialog = true },
                        modifier = Modifier.testTag("quiz_quit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Quiz"
                        )
                    }
                },
                actions = {
                    // Score chip
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(
                                color = if (correctCount >= 20) EmeraldGreen.copy(alpha = 0.15f) else RoyalBlue.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$correctCount / 20 to Pass",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (correctCount >= 20) EmeraldGreen else RoyalBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Overall Session Progress
            val overallProgress = ((currentIndex + 1).toFloat() / 30f)
            LinearProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = RoyalBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 15-SECOND COUNTDOWN TIMER DISPLAY
            val secondsRemaining = ((remainingMillis + 999) / 1000).coerceIn(0L, 15L).toInt()
            val timerFraction = (remainingMillis.toFloat() / timerDurationMs.toFloat()).coerceIn(0f, 1f)
            val timerColor = when {
                remainingMillis <= 4000L -> QuizRed
                remainingMillis <= 8000L -> WarmGold
                else -> EmeraldGreen
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .testTag("quiz_timer_indicator"),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { timerFraction },
                    modifier = Modifier.fillMaxSize(),
                    color = timerColor,
                    strokeWidth = 6.dp,
                    trackColor = Color(0xFFE2E8F0)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (questionState == QuestionState.TIMEOUT) "0" else "$secondsRemaining",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = timerColor,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "sec",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QUESTION CARD & TIMEOUT OVERLAY CONTAINER (BRIGHT, RADIANT & EYE-FRIENDLY)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 190.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(
                        width = 2.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFF2563EB), // Vibrant Royal Blue
                                Color(0xFFF59E0B), // Warm Gold
                                Color(0xFF2563EB)
                            )
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Category Pill with vibrant, bright badge & 10-Day Non-Repeat Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFEFF6FF),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = (currentQuestion?.category ?: category).uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1D4ED8),
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFF0FDF4),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "10-Day Non-Repeat",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large, bold, ultra-clear question text with maximum contrast to eliminate eye strain
                        Text(
                            text = currentQuestion?.questionText ?: "Loading question...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A), // Crisp Deep Slate/Navy for zero eye strain
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )

                        if (!currentQuestion?.bibleReference.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "📖 Ref: ${currentQuestion!!.bibleReference}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }

                // EXACT TIMEOUT EFFECT: PROMINENT LARGE RED "XXX" GRAPHIC & BUZZER OVERLAY
                if (questionState == QuestionState.TIMEOUT) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                color = QuizRedDark.copy(alpha = 0.96f),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .testTag("timeout_buzzer_overlay"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "XXX",
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TIME'S UP!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "15-Second Limit Exceeded",
                                fontSize = 13.sp,
                                color = Color(0xFFFFCDD2)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4 MULTIPLE CHOICE ANSWER BUTTONS (BRIGHT, DISTINCT COLORS & HIGH CONTRAST)
            val options = currentQuestion?.options ?: emptyList()
            options.forEachIndexed { optIndex, optionText ->
                val theme = OptionThemes.getOrElse(optIndex) { OptionThemes[0] }
                val isSelected = selectedOptionIndex == optIndex
                val isCorrect = optIndex == (currentQuestion?.correctOptionIndex?.toInt() ?: 0)
                val isLocked = questionState != QuestionState.COUNTING_DOWN

                val style = when {
                    isLocked && isCorrect -> {
                        // Correct Answer: Bright Glowing Emerald Green
                        OptionStyle(
                            cardBg = Color(0xFF059669), // Rich Emerald Green
                            cardBorder = Color(0xFF10B981), // Bright Mint Border
                            letterBg = Color.White,       // White Badge
                            letterText = Color(0xFF047857), // Green Letter
                            textColor = Color.White        // Crisp White Text
                        )
                    }
                    isLocked && isSelected && !isCorrect -> {
                        // Incorrect Selected: Bright Crimson Red
                        OptionStyle(
                            cardBg = Color(0xFFDC2626), // Vivid Red
                            cardBorder = Color(0xFFF87171), // Bright Red Border
                            letterBg = Color.White,       // White Badge
                            letterText = Color(0xFFDC2626), // Red Letter
                            textColor = Color.White        // Crisp White Text
                        )
                    }
                    isLocked -> {
                        // Other Options during feedback: Slightly Muted
                        OptionStyle(
                            cardBg = theme.cardBg.copy(alpha = 0.5f),
                            cardBorder = theme.borderColor.copy(alpha = 0.35f),
                            letterBg = theme.badgeBg.copy(alpha = 0.4f),
                            letterText = Color.White,
                            textColor = Color(0xFF64748B)
                        )
                    }
                    else -> {
                        // Active/Counting Down: Vibrant, Bright & Eye-Friendly!
                        OptionStyle(
                            cardBg = theme.cardBg,      // Bright Colored Pastel Container
                            cardBorder = theme.borderColor, // 2.5dp Bright Colored Border
                            letterBg = theme.badgeBg,     // Solid Bold Colored Badge
                            letterText = Color.White,       // Pure White Letter
                            textColor = Color(0xFF0F172A)  // Deep High-Contrast Slate Text
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(
                            width = if (isLocked && (isCorrect || isSelected)) 3.dp else 2.5.dp,
                            color = style.cardBorder,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable(enabled = questionState == QuestionState.COUNTING_DOWN) {
                            if (questionState == QuestionState.COUNTING_DOWN && SystemClock.elapsedRealtime() < deadlineMs) {
                                selectedOptionIndex = optIndex
                                questionState = QuestionState.ANSWER_SUBMITTED
                                if (optIndex == currentQuestion?.correctOptionIndex?.toInt()) {
                                    correctCount++
                                }
                                questionState = QuestionState.FEEDBACK
                            }
                        }
                        .testTag("quiz_option_$optIndex"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = style.cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isLocked && isCorrect) 6.dp else 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bright Colored Badge (A, B, C, D)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(style.letterBg, shape = CircleShape)
                                .border(1.5.dp, if (isLocked && (isCorrect || isSelected)) Color.White else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLocked && isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Correct",
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (isLocked && isSelected && !isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Incorrect",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = theme.letter,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = style.letterText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Large, bold, ultra-readable answer text
                        Text(
                            text = optionText,
                            fontSize = 17.sp,
                            fontWeight = if (isLocked && (isCorrect || isSelected)) FontWeight.ExtraBold else FontWeight.Bold,
                            color = style.textColor,
                            lineHeight = 23.sp,
                            modifier = Modifier.weight(1f)
                        )

                        if (isLocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isCorrect) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                        .padding(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Correct",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                        .padding(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Incorrect",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ADVERTISING CHECKPOINT DIALOG (Triggered after every 2 resolved questions - 2 Rewarded Ads required before continuing)
    if (showAdCheckpoint) {
        AlertDialog(
            onDismissRequest = {
                // Must complete 2 rewarded ads before dismissing dialog
                if (checkpointRewardedAdsWatched >= 2) {
                    showAdCheckpoint = false
                    checkpointRewardedAdsWatched = 0
                    proceedToNextAfterAd(
                        currentIndex = currentIndex,
                        totalCount = sessionQuestions.size,
                        onNext = {
                            currentIndex++
                            selectedOptionIndex = null
                            questionState = QuestionState.COUNTING_DOWN
                        },
                        onComplete = {
                            questionState = QuestionState.COMPLETED
                            onQuizComplete(correctCount, timedOutCount)
                        }
                    )
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = RoyalBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stage Checkpoint", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Checkpoint reached after Question ${currentIndex + 1} (every 2 answered questions).",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Watch 2 rewarded ads to unlock the next question and earn bonus points!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress indicators for the 2 required rewarded ads
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Ad 1 Badge
                        val isAd1Done = checkpointRewardedAdsWatched >= 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isAd1Done) EmeraldGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isAd1Done) EmeraldGreen else Color.Gray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isAd1Done) "✓ Ad 1 Completed" else "⏳ Ad 1 Required",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAd1Done) EmeraldGreen else Color.DarkGray
                            )
                        }

                        // Ad 2 Badge
                        val isAd2Done = checkpointRewardedAdsWatched >= 2
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isAd2Done) EmeraldGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isAd2Done) EmeraldGreen else Color.Gray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isAd2Done) "✓ Ad 2 Completed" else "⏳ Ad 2 Required",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAd2Done) EmeraldGreen else Color.DarkGray
                            )
                        }
                    }

                    if (adErrorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = adErrorMessage!!,
                            color = QuizRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    NativeAdCard(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    BannerAdView(modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (checkpointRewardedAdsWatched < 2) {
                        Button(
                            onClick = {
                                if (!isAdLoadingOrShowing) {
                                    isAdLoadingOrShowing = true
                                    adErrorMessage = null
                                    if (onShowRewardedAd != null) {
                                        onShowRewardedAd { success ->
                                            isAdLoadingOrShowing = false
                                            if (success) {
                                                checkpointRewardedAdsWatched++
                                                adErrorMessage = null
                                            } else {
                                                adErrorMessage = "Ad is loading or unavailable. Please try again."
                                            }
                                        }
                                    } else {
                                        // Fallback for previews/tests
                                        isAdLoadingOrShowing = false
                                        checkpointRewardedAdsWatched++
                                    }
                                }
                            },
                            enabled = !isAdLoadingOrShowing,
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("watch_rewarded_ad_button")
                        ) {
                            if (isAdLoadingOrShowing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading Ad...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Watch Rewarded Ad (${checkpointRewardedAdsWatched + 1} of 2)")
                            }
                        }

                        // Graceful recovery option if ad fails to load repeatedly
                        if (adErrorMessage != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = {
                                    showAdCheckpoint = false
                                    checkpointRewardedAdsWatched = 0
                                    proceedToNextAfterAd(
                                        currentIndex = currentIndex,
                                        totalCount = sessionQuestions.size,
                                        onNext = {
                                            currentIndex++
                                            selectedOptionIndex = null
                                            questionState = QuestionState.COUNTING_DOWN
                                        },
                                        onComplete = {
                                            questionState = QuestionState.COMPLETED
                                            onQuizComplete(correctCount, timedOutCount)
                                        }
                                    )
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = "Skip Checkpoint (Ad Unavailable)",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                showAdCheckpoint = false
                                checkpointRewardedAdsWatched = 0
                                proceedToNextAfterAd(
                                    currentIndex = currentIndex,
                                    totalCount = sessionQuestions.size,
                                    onNext = {
                                        currentIndex++
                                        selectedOptionIndex = null
                                        questionState = QuestionState.COUNTING_DOWN
                                    },
                                    onComplete = {
                                        questionState = QuestionState.COMPLETED
                                        onQuizComplete(correctCount, timedOutCount)
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ad_continue_button")
                        ) {
                            Text(
                                text = if (currentIndex + 1 >= sessionQuestions.size) "✓ Complete Quiz & View Results" else "✓ Continue to Next Question",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        )
    }

    // Quit Quiz Confirmation Dialog
    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text("Leave Quiz?") },
            text = {
                Text("If you leave now, this 30-question session will be abandoned and no points will be awarded.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitDialog = false
                        onExitQuiz()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QuizRed)
                ) {
                    Text("Quit")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showQuitDialog = false }) {
                    Text("Resume")
                }
            }
        )
    }
}

private fun handleAdvanceAfterQuestion(
    currentIndex: Int,
    totalCount: Int,
    onShowAd: () -> Unit,
    onNextQuestion: () -> Unit,
    onComplete: () -> Unit
) {
    val questionNumber = currentIndex + 1
    // Checkpoint schedule: every 2 resolved questions (Q2, Q4, Q6, ..., Q30)
    val isCheckpointDue = (questionNumber % 2 == 0)

    if (isCheckpointDue) {
        onShowAd()
    } else {
        if (questionNumber >= totalCount) {
            onComplete()
        } else {
            onNextQuestion()
        }
    }
}

private fun proceedToNextAfterAd(
    currentIndex: Int,
    totalCount: Int,
    onNext: () -> Unit,
    onComplete: () -> Unit
) {
    if (currentIndex + 1 >= totalCount) {
        onComplete()
    } else {
        onNext()
    }
}
