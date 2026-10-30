package com.bibletriviaquiz.playandwin.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibletriviaquiz.playandwin.data.repository.ClaimResult
import com.bibletriviaquiz.playandwin.data.repository.ReferralRepository
import com.bibletriviaquiz.playandwin.model.Referral
import com.bibletriviaquiz.playandwin.model.UserProfile
import com.bibletriviaquiz.playandwin.ui.theme.EmeraldGreen
import com.bibletriviaquiz.playandwin.ui.theme.EmeraldGreenContainer
import com.bibletriviaquiz.playandwin.ui.theme.GoldenBronze
import com.bibletriviaquiz.playandwin.ui.theme.RoyalBlue
import com.bibletriviaquiz.playandwin.ui.theme.RoyalBlueContainer
import com.bibletriviaquiz.playandwin.ui.theme.RoyalNavy
import com.bibletriviaquiz.playandwin.ui.theme.SlateBackground
import com.bibletriviaquiz.playandwin.ui.theme.WarmGold
import com.bibletriviaquiz.playandwin.ui.theme.WarmGoldContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
    userProfile: UserProfile?,
    referralRepository: ReferralRepository,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val myReferrals by referralRepository.observeMyReferrals().collectAsState(initial = emptyList())
    val myReferralAsReferee by referralRepository.observeMyReferralAsReferee().collectAsState(initial = null)

    var referralCode by remember {
        mutableStateOf(userProfile?.referralCode.takeIf { !it.isNullOrBlank() } ?: referralRepository.deriveReferralCode())
    }
    val referralLink = remember(referralCode) {
        referralRepository.buildReferralLink(referralCode)
    }

    var showInfoDialog by remember { mutableStateOf(false) }
    var inputCode by remember { mutableStateOf("") }
    var isApplyingCode by remember { mutableStateOf(false) }
    var isCheckingBonuses by remember { mutableStateOf(false) }
    var claimSuccessDialog by remember { mutableStateOf<ClaimResult?>(null) }

    // Ensure referral code is registered in Firestore
    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            val registered = referralRepository.ensureReferralCodeRegistered(
                displayName = userProfile.displayName.ifBlank { "Bible Scholar" }
            )
            referralCode = registered
        }
    }

    // Auto check if any qualified bonuses can be claimed
    LaunchedEffect(myReferrals) {
        val hasUnclaimed = myReferrals.any { it.status != "credited" && it.refereePoints >= 10000L }
        if (hasUnclaimed) {
            val result = referralRepository.checkAndClaimReferralBonuses()
            result.onSuccess { claimRes ->
                if (claimRes.claimedCount > 0) {
                    claimSuccessDialog = claimRes
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Refer & Earn 100 Pts",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("referral_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.testTag("referral_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Program Rules",
                            tint = WarmGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RoyalNavy
                )
            )
        },
        containerColor = SlateBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Hero Card: Explain 100 Pts / 10,000 Pts Rule
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("referral_hero_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(RoyalNavy, RoyalBlue, Color(0xFF172554))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(WarmGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CardGiftcard,
                                        contentDescription = null,
                                        tint = RoyalNavy,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Invite Friends • Earn 100 Pts",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Share God's Word & Earn Rewards",
                                        fontSize = 12.sp,
                                        color = Color(0xFF93C5FD)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Give your friends your personalized invite link. When they join and reach at least 10,000 points by completing quizzes, you will be credited 100 bonus points to your wallet!",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                lineHeight = 19.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 3-step visual pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StepPill(step = "1", text = "Share Link", modifier = Modifier.weight(1f))
                                StepPill(step = "2", text = "Friend Joins", modifier = Modifier.weight(1f))
                                StepPill(step = "3", text = "10k Pts Earned", modifier = Modifier.weight(1.1f))
                                StepPill(step = "4", text = "+100 Pts!", isHighlight = true, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 2. Personalized Link & Code Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("referral_link_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "Your Personalized Invite Code",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RoyalBlueContainer, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = referralCode,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RoyalNavy,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = referralLink,
                                    fontSize = 11.sp,
                                    color = RoyalBlue,
                                    maxLines = 1
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        copyToClipboard(context, referralCode, "Referral Code")
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Copied referral code: $referralCode")
                                        }
                                    },
                                    modifier = Modifier.testTag("copy_code_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Code",
                                        tint = RoyalBlue
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: Share Link & Copy Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    shareReferralLink(context, referralCode, referralLink)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("share_referral_link_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WarmGold,
                                    contentColor = RoyalNavy
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Share Link",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    copyToClipboard(context, referralLink, "Invite Link")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Copied personalized link to clipboard!")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("copy_referral_link_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Copy Link",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Stats Overview Card
            val totalReferred = myReferrals.size
            val qualifiedCount = myReferrals.count { it.refereePoints >= 10000L }
            val creditedCount = myReferrals.count { it.status == "credited" }
            val totalEarnedPoints = creditedCount * 100L

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Your Referral Performance",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalNavy
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatBox(
                                label = "Friends Joined",
                                value = "$totalReferred",
                                icon = Icons.Default.People,
                                tint = RoyalBlue
                            )
                            StatBox(
                                label = "Reached 10k Pts",
                                value = "$qualifiedCount",
                                icon = Icons.Default.MilitaryTech,
                                tint = EmeraldGreen
                            )
                            StatBox(
                                label = "Points Earned",
                                value = "+$totalEarnedPoints",
                                icon = Icons.Default.Star,
                                tint = GoldenBronze
                            )
                        }

                        // Check & Claim Bonuses Button
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                isCheckingBonuses = true
                                coroutineScope.launch {
                                    val result = referralRepository.checkAndClaimReferralBonuses()
                                    isCheckingBonuses = false
                                    result.onSuccess { claimRes ->
                                        if (claimRes.claimedCount > 0) {
                                            claimSuccessDialog = claimRes
                                        } else {
                                            snackbarHostState.showSnackbar("All earned referral rewards are currently up-to-date!")
                                        }
                                    }.onFailure { err ->
                                        snackbarHostState.showSnackbar("Could not check bonuses: ${err.message}")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("check_claim_bonuses_button"),
                            enabled = !isCheckingBonuses,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Color.White
                            )
                        ) {
                            if (isCheckingBonuses) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Check & Claim 100 Pts Bonuses",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. If current user was referred by someone, show their own progress toward helping their referrer!
            if (myReferralAsReferee != null) {
                item {
                    val ref = myReferralAsReferee!!
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = WarmGoldContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = GoldenBronze,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Your Progress As An Invited Friend",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalNavy
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "You joined with invite code ${ref.referrerCode}. When you earn 10,000 points, your referrer receives 100 points!",
                                fontSize = 12.sp,
                                color = RoyalNavy.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { ref.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = WarmGold,
                                trackColor = WarmGold.copy(alpha = 0.2f),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "%,d / 10,000 pts".format(ref.refereePoints),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalNavy
                                )
                                Text(
                                    text = "${(ref.progressFraction * 100).toInt()}% towards goal",
                                    fontSize = 11.sp,
                                    color = GoldenBronze,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 5. Enter Friend's Referral Code (if not yet referred)
            val alreadyReferred = userProfile?.referredByUserId != null || myReferralAsReferee != null
            if (!alreadyReferred) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_referral_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Have A Friend's Invite Code?",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalNavy
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enter their code or paste their invite link to support them.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = inputCode,
                                    onValueChange = { input ->
                                        // If user pastes full link, extract code query param
                                        val extracted = if (input.contains("code=")) {
                                            input.substringAfter("code=").substringBefore("&").trim()
                                        } else {
                                            input.trim()
                                        }
                                        inputCode = extracted
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("referral_code_input"),
                                    placeholder = { Text("e.g. REF-ABC123", fontSize = 13.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RoyalBlue,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (inputCode.isBlank()) return@Button
                                        isApplyingCode = true
                                        coroutineScope.launch {
                                            val res = referralRepository.applyReferralCode(inputCode)
                                            isApplyingCode = false
                                            res.onSuccess { msg ->
                                                inputCode = ""
                                                snackbarHostState.showSnackbar(msg)
                                            }.onFailure { err ->
                                                snackbarHostState.showSnackbar(err.message ?: "Failed to apply referral code")
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("apply_referral_code_button"),
                                    enabled = !isApplyingCode && inputCode.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoyalBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    if (isApplyingCode) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Apply", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Friends You Referred List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Friends You Referred (${myReferrals.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "10k pts needed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (myReferrals.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No friends referred yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Share Link' above to invite your friends to Bible Trivia Quiz: Play & Win and earn 100 points when they reach 10,000 points!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                items(myReferrals) { referral ->
                    ReferredFriendCard(
                        referral = referral,
                        onSimulate10k = {
                            coroutineScope.launch {
                                val simRes = referralRepository.simulateRefereePointsForTesting(
                                    referralId = referral.referralId,
                                    targetPoints = 10000L
                                )
                                simRes.onSuccess {
                                    snackbarHostState.showSnackbar("Simulated ${referral.refereeDisplayName} reaching 10,000 pts! Tap 'Check & Claim' to receive your 100 pts.")
                                }.onFailure { e ->
                                    snackbarHostState.showSnackbar("Simulation note: ${e.message}")
                                }
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Info Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text("Referral System Rules", fontWeight = FontWeight.Bold, color = RoyalNavy)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. Personal Invite Link: Every user has a unique referral code and personalized link.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "2. Verified Sign-Ups: When a new friend signs up using your link or code via Google Sign-In, they become a verified referral.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "3. 10,000 Points Requirement: The 100 points reward is only credited to your wallet once your referred friend has earned at least 10,000 points by engaging with the Bible quiz system.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldenBronze
                    )
                    Text(
                        text = "4. Automated Crediting: Once the 10,000 points threshold is reached, you can claim your 100 points reward or have it automatically credited to your wallet with a recorded transaction ledger.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got It", fontWeight = FontWeight.Bold, color = RoyalBlue)
                }
            }
        )
    }

    // Celebration Dialog when 100-point rewards are claimed
    if (claimSuccessDialog != null) {
        val res = claimSuccessDialog!!
        AlertDialog(
            onDismissRequest = { claimSuccessDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = null,
                    tint = WarmGold,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text("🎉 +${res.pointsAwarded} Points Credited!", fontWeight = FontWeight.Bold, color = RoyalNavy)
            },
            text = {
                Column {
                    Text(
                        text = "Congratulations! ${res.claimedCount} of your referred friends have reached at least 10,000 points:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    res.friendNames.forEach { name ->
                        Text(
                            text = "• $name (10,000+ pts earned)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${res.pointsAwarded} points have been credited directly to your Points Wallet balance!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = RoyalNavy
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { claimSuccessDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = RoyalNavy)
                ) {
                    Text("Awesome!", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun StepPill(
    step: String,
    text: String,
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                if (isHighlight) WarmGold else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Step $step",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) RoyalNavy else Color(0xFF93C5FD)
            )
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) RoyalNavy else Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RoyalNavy
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReferredFriendCard(
    referral: Referral,
    onSimulate10k: () -> Unit
) {
    val progress = referral.progressFraction
    val isQualified = referral.refereePoints >= 10000L
    val isCredited = referral.status == "credited"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("referred_friend_${referral.referralId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(RoyalBlueContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = referral.refereeDisplayName.take(1).uppercase().ifBlank { "F" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = referral.refereeDisplayName.ifBlank { "Referred Friend" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalNavy
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified User",
                                tint = RoyalBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (referral.refereeEmail.isNotBlank()) {
                            Text(
                                text = maskEmail(referral.refereeEmail),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Status Chip
                when {
                    isCredited -> {
                        Box(
                            modifier = Modifier
                                .background(RoyalBlueContainer, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+100 Pts Credited",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue
                                )
                            }
                        }
                    }
                    isQualified -> {
                        Box(
                            modifier = Modifier
                                .background(EmeraldGreenContainer, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MilitaryTech,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "100 Pts Ready!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .background(WarmGoldContainer, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = GoldenBronze,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "In Progress",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldenBronze
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Points Progress Bar towards 10,000 points
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (isQualified) EmeraldGreen else WarmGold,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "%,d / 10,000 pts".format(referral.refereePoints),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isQualified) EmeraldGreen else RoyalNavy
                )
                Text(
                    text = if (isQualified) "Target reached (100%)" else "${(progress * 100).toInt()}% completed",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Simulate Button for Testing / Verification
            if (!isQualified && !isCredited) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onSimulate10k,
                        modifier = Modifier.testTag("simulate_10k_${referral.referralId}")
                    ) {
                        Text(
                            text = "⚡ Simulate 10k Pts (Test)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue
                        )
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}

private fun shareReferralLink(context: Context, code: String, link: String) {
    val message = "Join me on Bible Trivia Quiz: Play & Win! Test your Bible knowledge, earn points, and redeem physical rewards in the shop.\n\nUse my personal invite link: $link\nOr enter my invite code: $code"
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, message)
        putExtra(Intent.EXTRA_SUBJECT, "Join Bible Trivia Quiz: Play & Win!")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Referral Link via")
    context.startActivity(shareIntent)
}

private fun maskEmail(email: String): String {
    val parts = email.split("@")
    if (parts.size != 2) return email
    val name = parts[0]
    val domain = parts[1]
    val maskedName = if (name.length <= 2) name else name.take(2) + "***"
    return "$maskedName@$domain"
}
