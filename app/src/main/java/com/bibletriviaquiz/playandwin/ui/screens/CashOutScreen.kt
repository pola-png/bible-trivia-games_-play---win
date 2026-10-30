package com.bibletriviaquiz.playandwin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibletriviaquiz.playandwin.model.CashRedemptionRequest
import com.bibletriviaquiz.playandwin.model.UserProfile
import com.bibletriviaquiz.playandwin.ui.theme.EmeraldGreen
import com.bibletriviaquiz.playandwin.ui.theme.GoldenBronze
import com.bibletriviaquiz.playandwin.ui.theme.QuizRed
import com.bibletriviaquiz.playandwin.ui.theme.RoyalBlue
import com.bibletriviaquiz.playandwin.ui.theme.RoyalNavy
import com.bibletriviaquiz.playandwin.ui.theme.WarmGold
import com.bibletriviaquiz.playandwin.ui.theme.WarmGoldContainer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashOutScreen(
    userProfile: UserProfile?,
    daysActive: Long,
    cashRequests: List<CashRedemptionRequest>,
    onRequestCashOut: suspend (points: Long, name: String, number: String, bank: String, method: String) -> Result<CashRedemptionRequest>,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val currentPoints = userProfile?.currentPoints ?: 0L
    val minPoints = 50000L
    val minDays = 60L

    val hasEnoughPoints = currentPoints >= minPoints
    val hasEnoughDays = daysActive >= minDays
    val isEligible = hasEnoughPoints && hasEnoughDays

    var pointsToRedeem by remember { mutableLongStateOf(50000L) }
    var accountName by remember { mutableStateOf(userProfile?.displayName ?: "") }
    var accountNumber by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var payoutMethod by remember { mutableStateOf("Bank Transfer") }
    var isSubmitting by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val usdEquivalent = pointsToRedeem / 1000L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Redeem Points for Cash", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("cashout_back_button")) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CRITICAL SPIRITUAL MOTIVATION DISCLAIMER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WarmGoldContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(GoldenBronze.copy(alpha = 0.2f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = GoldenBronze, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Important Notice: Not a Money-Making App",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "This is NOT a commercial money-making application. The cash reward is a simple motivation to honor and reward dedication to studying God's Holy Word.\n\nThe primary purpose of this app is to inspire believers to deepen their biblical knowledge and cultivate an intimate personal relationship with God.",
                                fontSize = 12.sp,
                                color = RoyalNavy.copy(alpha = 0.85f),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // Dual Eligibility Tracker Card (Points & 60 Days Active)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Cash Out Eligibility Requirements",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Requirement 1: 50,000 Points
                        val pointsProgress = (currentPoints.toFloat() / minPoints.toFloat()).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (hasEnoughPoints) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (hasEnoughPoints) EmeraldGreen else QuizRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Points Balance", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = "%,d / 50,000 pts ($50 USD)".format(currentPoints),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasEnoughPoints) EmeraldGreen else RoyalBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { pointsProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (hasEnoughPoints) EmeraldGreen else RoyalBlue,
                            trackColor = Color(0xFFE2E8F0)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Requirement 2: 60 Days Active
                        val daysProgress = (daysActive.toFloat() / minDays.toFloat()).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (hasEnoughDays) Icons.Default.CheckCircle else Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (hasEnoughDays) EmeraldGreen else GoldenBronze,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("60-Day Active Membership", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = "$daysActive / 60 days active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasEnoughDays) EmeraldGreen else GoldenBronze
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { daysProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (hasEnoughDays) EmeraldGreen else GoldenBronze,
                            trackColor = Color(0xFFE2E8F0)
                        )

                        if (!hasEnoughDays) {
                            val daysLeft = minDays - daysActive
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$daysLeft days remaining before cash out unlocks. Continue daily Bible study!",
                                fontSize = 11.sp,
                                color = GoldenBronze
                            )
                        }
                    }
                }
            }

            // Cash Out Request Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Submit Cash Out Request",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Conversion rate: 10,000 points = $10 USD (min $50 USD)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Select Points Amount:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            listOf(50000L, 60000L, 75000L, 100000L).forEach { pts ->
                                FilterChip(
                                    selected = pointsToRedeem == pts,
                                    onClick = { pointsToRedeem = pts },
                                    label = { Text("%,d pts ($%d)".format(pts, pts / 1000L), fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cash Payout Equivalent:", fontSize = 13.sp, color = Color(0xFF475569))
                                Text(
                                    text = "$$usdEquivalent.00 USD",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = accountName,
                            onValueChange = { accountName = it },
                            label = { Text("Account Holder Full Name") },
                            modifier = Modifier.fillMaxWidth().testTag("cashout_name_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            label = { Text("Account Number / IBAN / PayPal Email") },
                            modifier = Modifier.fillMaxWidth().testTag("cashout_number_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name / Payout Provider (e.g. Zenith Bank, PayPal)") },
                            modifier = Modifier.fillMaxWidth().testTag("cashout_bank_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                if (accountName.isBlank() || accountNumber.isBlank() || bankName.isBlank()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Please enter all payout account details.")
                                    }
                                    return@Button
                                }
                                isSubmitting = true
                                coroutineScope.launch {
                                    val result = onRequestCashOut(pointsToRedeem, accountName, accountNumber, bankName, payoutMethod)
                                    isSubmitting = false
                                    if (result.isSuccess) {
                                        snackbarHostState.showSnackbar("Cash out request submitted successfully! Pending admin approval.")
                                    } else {
                                        snackbarHostState.showSnackbar(result.exceptionOrNull()?.localizedMessage ?: "Failed to submit request")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("submit_cashout_button"),
                            enabled = isEligible && !isSubmitting,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEligible) EmeraldGreen else Color(0xFF94A3B8),
                                contentColor = Color.White
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (isEligible) "Apply for $$usdEquivalent USD Cash Out" else "Requirements Not Met",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Past Cash Out Requests List
            item {
                Text(
                    text = "Cash Out Request History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (cashRequests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("No cash out requests yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(cashRequests) { req ->
                    val statusColor = when (req.status) {
                        "processed", "approved" -> EmeraldGreen
                        "rejected" -> QuizRed
                        else -> GoldenBronze
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "$${req.usdAmount} USD (${req.bankName})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "%,d points redeemed".format(req.pointsRedeemed),
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                if (!req.paymentReference.isNullOrBlank()) {
                                    Text(
                                        text = "Ref: ${req.paymentReference}",
                                        fontSize = 11.sp,
                                        color = EmeraldGreen
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = req.status.replaceFirstChar { it.uppercase() },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = statusColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
