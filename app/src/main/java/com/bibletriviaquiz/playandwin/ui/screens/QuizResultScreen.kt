package com.bibletriviaquiz.playandwin.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Wallet
import com.bibletriviaquiz.playandwin.ads.BannerAdView
import com.bibletriviaquiz.playandwin.ads.NativeAdCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibletriviaquiz.playandwin.ui.theme.EmeraldGreen
import com.bibletriviaquiz.playandwin.ui.theme.QuizRed
import com.bibletriviaquiz.playandwin.ui.theme.RoyalBlue
import com.bibletriviaquiz.playandwin.ui.theme.RoyalNavy
import com.bibletriviaquiz.playandwin.ui.theme.WarmGold

@Composable
fun QuizResultScreen(
    correctCount: Int,
    timedOutCount: Int,
    totalQuestions: Int = 30,
    onPlayAgainClick: () -> Unit,
    onWalletClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    val isPassed = correctCount >= 20
    val pointsAwarded = if (isPassed) 50 else 0
    val scorePercentage = ((correctCount.toDouble() / totalQuestions.toDouble()) * 100).toInt()
    val incorrectCount = totalQuestions - correctCount

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isPassed) {
                        listOf(Color(0xFF064E3B), EmeraldGreen, Color(0xFF0F2B5C))
                    } else {
                        listOf(RoyalNavy, RoyalBlue, Color(0xFF1E293B))
                    }
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Trophy / Status Badge
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = if (isPassed) WarmGold else Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPassed) Icons.Default.EmojiEvents else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isPassed) RoyalNavy else Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isPassed) "Congratulations! You Passed!" else "Keep Learning and Try Again!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isPassed) {
                    "You achieved the required 20+ passing score and advanced to the next stage!"
                } else {
                    "You need at least 20 correct answers out of 30 to earn 50 points and progress to another stage."
                },
                fontSize = 14.sp,
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Points Reward Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("result_points_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPassed) WarmGold else Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = if (isPassed) RoyalNavy else Color(0xFF64748B),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isPassed) "+50 Points Earned!" else "0 Points Earned",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPassed) RoyalNavy else RoyalNavy
                        )
                        Text(
                            text = if (isPassed) "Added to your wallet • Next stage unlocked" else "Score 20+ correct to earn 50 points & progress",
                            fontSize = 12.sp,
                            color = if (isPassed) RoyalNavy.copy(alpha = 0.8f) else Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Detailed Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Session Summary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalNavy
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    ResultRow(
                        label = "Correct Answers",
                        value = "$correctCount / $totalQuestions",
                        color = EmeraldGreen,
                        icon = Icons.Default.CheckCircle
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ResultRow(
                        label = "Incorrect Answers",
                        value = "$incorrectCount / $totalQuestions",
                        color = QuizRed,
                        icon = Icons.Default.Close
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ResultRow(
                        label = "Timed Out (3s Buzzer)",
                        value = "$timedOutCount",
                        color = Color(0xFFD97706),
                        icon = Icons.Default.TimerOff
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ResultRow(
                        label = "Overall Accuracy",
                        value = "$scorePercentage%",
                        color = RoyalBlue,
                        icon = Icons.Default.EmojiEvents
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sponsored Native Ad
            NativeAdCard(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Button(
                onClick = onPlayAgainClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("play_again_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPassed) WarmGold else Color.White,
                    contentColor = RoyalNavy
                )
            ) {
                Icon(imageVector = Icons.Default.Replay, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Another Quiz", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onWalletClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("view_wallet_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Wallet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Wallet", fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = onHomeClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("return_home_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Home", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            BannerAdView(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFF475569)
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
