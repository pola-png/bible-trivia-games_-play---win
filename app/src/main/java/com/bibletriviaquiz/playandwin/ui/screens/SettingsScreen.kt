package com.bibletriviaquiz.playandwin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibletriviaquiz.playandwin.audio.BuzzerManager
import com.bibletriviaquiz.playandwin.ui.theme.EmeraldGreen
import com.bibletriviaquiz.playandwin.ui.theme.GoldenBronze
import com.bibletriviaquiz.playandwin.ui.theme.QuizRed
import com.bibletriviaquiz.playandwin.ui.theme.RoyalBlue
import com.bibletriviaquiz.playandwin.ui.theme.RoyalNavy
import com.bibletriviaquiz.playandwin.ui.theme.WarmGold
import com.bibletriviaquiz.playandwin.ui.theme.WarmGoldContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userEmail: String,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit = onSignOutClick,
    onReferralClick: () -> Unit = {},
    onBackClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var isSoundEnabled by remember { mutableStateOf(!BuzzerManager.isMuted()) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Rules", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // User Account Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Signed In Account", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(userEmail.ifBlank { "User" }, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Audio & Buzzer Settings
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = RoyalBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Quiz Timeout Buzzer Sound", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Play sound effect on question timeout", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isSoundEnabled,
                            onCheckedChange = {
                                isSoundEnabled = it
                                BuzzerManager.setMuted(!it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = RoyalBlue, checkedTrackColor = RoyalBlue.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            // Referral Program Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReferralClick() }
                        .testTag("settings_referral_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmGoldContainer.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CardGiftcard, contentDescription = null, tint = GoldenBronze)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Referral Program & Invite Friends", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = RoyalNavy)
                                Text("Earn 100 points when your friend reaches 10k points", fontSize = 12.sp, color = GoldenBronze)
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RoyalNavy)
                    }
                }
            }

            // Legal & Rules Section
            item {
                Text("Legal & Policies", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsRow(icon = Icons.Default.HelpOutline, title = "Bible Quiz Rules & Passing Standard") {
                            showPolicyDialog = "rules"
                        }
                        SettingsRow(icon = Icons.Default.Info, title = "Rewards Shop & Redemption Policy") {
                            showPolicyDialog = "redemption"
                        }
                        SettingsRow(icon = Icons.Default.Description, title = "Terms & Conditions") {
                            showPolicyDialog = "terms"
                        }
                        SettingsRow(icon = Icons.Default.PrivacyTip, title = "Privacy Policy") {
                            showPolicyDialog = "privacy"
                        }
                        SettingsRow(icon = Icons.Default.DeleteForever, title = "Account & Data Deletion Policy") {
                            showPolicyDialog = "deletion"
                        }
                    }
                }
            }

            // Sign Out & Account Deletion
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }

            item {
                OutlinedButton(
                    onClick = { showDeleteAccountDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("delete_account_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = QuizRed)
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account & Data")
                }
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of Bible Trivia Quiz: Play & Win?") },
            confirmButton = {
                Button(onClick = {
                    showSignOutDialog = false
                    onSignOutClick()
                }) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account & Data", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to permanently delete your account?\n\n" +
                    "This action cannot be undone. In accordance with Google Play User Data policies, " +
                    "your profile, accumulated points, quiz session performance history, and saved delivery addresses " +
                    "will be permanently purged from our servers."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccountClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QuizRed)
                ) {
                    Text("Permanently Delete Account")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPolicyDialog != null) {
        val (title, content, webUrl) = when (showPolicyDialog) {
            "rules" -> Triple(
                "Bible Quiz Rules",
                "1. Session Length: Each quiz session contains exactly 30 questions.\n\n" +
                "2. 15-Second Monotonic Timer: An authoritative 15-second countdown timer runs on every question. " +
                "Unanswered questions trigger the buzzer sound and are counted as incorrect/timed-out.\n\n" +
                "3. Passing Standard: You must answer at least 20 out of 30 questions correctly to pass the stage.\n\n" +
                "4. Point Rewards: Passing sessions (20+ correct) award exactly 50 bonus points and advance your category cycle. " +
                "Sessions with under 20 correct answers award 0 points.\n\n" +
                "5. Checkpoint Ads: After every 2 answered questions (Q2, Q4, ..., Q30), 2 rewarded video ads must be viewed to continue.\n\n" +
                "6. Question Bank & Guarantee: Verified biblical database with over 100,000 questions across 15 categories. " +
                "Questions are guaranteed never to repeat for at least 10 consecutive days.",
                "https://woven-art-402909.web.app/terms.html"
            )
            "redemption" -> Triple(
                "Rewards & Redemption Policy",
                "1. Non-Monetary Nature: Points earned are purely promotional and have NO direct cash value. " +
                "Points cannot be sold, transferred, or exchanged for fiat currency or cryptocurrency.\n\n" +
                "2. Physical Merchandise: Points can only be exchanged for physical items available in the Rewards Shop.\n\n" +
                "3. Minimum Threshold: A minimum of 10,000 points is required to place a redemption order.\n\n" +
                "4. Delivery Coverage: Dispatched via domestic parcel couriers to verified addresses across Nigerian states. " +
                "Estimated dispatch time is 3 to 7 business days.",
                "https://woven-art-402909.web.app/terms.html"
            )
            "terms" -> Triple(
                "Terms & Conditions",
                "Bible Trivia Quiz: Play & Win is an educational Christian trivia application operated by Ubyy Tech.\n\n" +
                "• User Conduct: You agree to play fairly. The use of bots, emulators, auto-clickers, proxy servers, " +
                "or fraudulent ad interactions is strictly prohibited and results in permanent account suspension and point forfeiture.\n\n" +
                "• Account Responsibility: You are responsible for maintaining the confidentiality of your sign-in credentials.\n\n" +
                "• Limitation of Liability: Provided on an 'as-is' and 'as-available' basis without warranties.",
                "https://woven-art-402909.web.app/terms.html"
            )
            "deletion" -> Triple(
                "Account & Data Deletion Policy",
                "In compliance with Google Play User Data policies:\n\n" +
                "• In-App Deletion: You can instantly delete your account by scrolling to the bottom of this screen and tapping 'Delete Account & Data'.\n\n" +
                "• Web Deletion Portal: If you have uninstalled the app or lost device access, visit our public web portal at:\n" +
                "https://woven-art-402909.web.app/delete-account.html\n\n" +
                "• Data Deleted: Email, display name, User ID, accumulated points, quiz history, and postal addresses.\n\n" +
                "• Timeline: In-app deletions take effect immediately. Web requests are processed within 24-48 hours.",
                "https://woven-art-402909.web.app/delete-account.html"
            )
            else -> Triple(
                "Privacy Policy",
                "Bible Trivia Quiz: Play & Win (Ubyy Tech) is committed to protecting your privacy:\n\n" +
                "• Data Collected: Email, display name, and User ID for authentication; quiz performance statistics for fair gameplay; " +
                "delivery address and contact phone number solely for physical product shipping.\n\n" +
                "• Third-Party SDKs: We use Google Firebase (authentication and secure cloud database) and Google Mobile Ads (AdMob) " +
                "to display banner, native, and rewarded video ads.\n\n" +
                "• Data Security: All network traffic is encrypted via HTTPS/TLS in transit.\n\n" +
                "• Children's Privacy: General audience app (13+). We do not knowingly collect personal data from children under 13.\n\n" +
                "• Data Rights: You have the right to access, rectify, or permanently delete your account and personal data at any time.",
                "https://woven-art-402909.web.app/privacy-policy.html"
            )
        }

        AlertDialog(
            onDismissRequest = { showPolicyDialog = null },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(content, fontSize = 13.sp, lineHeight = 19.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showPolicyDialog = null }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = { uriHandler.openUri(webUrl) }) {
                    Text("Open Web Page")
                }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        }
    }
}
