package com.bibletriviaquiz.playandwin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.bibletriviaquiz.playandwin.data.repository.AdminRepository
import com.bibletriviaquiz.playandwin.model.AdminWithdrawalDetail
import com.bibletriviaquiz.playandwin.model.Order
import com.bibletriviaquiz.playandwin.model.Product
import com.bibletriviaquiz.playandwin.model.UserProfile
import com.bibletriviaquiz.playandwin.ui.theme.EmeraldGreen
import com.bibletriviaquiz.playandwin.ui.theme.EmeraldGreenContainer
import com.bibletriviaquiz.playandwin.ui.theme.GoldenBronze
import com.bibletriviaquiz.playandwin.ui.theme.QuizRed
import com.bibletriviaquiz.playandwin.ui.theme.QuizRedContainer
import com.bibletriviaquiz.playandwin.ui.theme.RoyalBlue
import com.bibletriviaquiz.playandwin.ui.theme.RoyalBlueContainer
import com.bibletriviaquiz.playandwin.ui.theme.RoyalNavy
import com.bibletriviaquiz.playandwin.ui.theme.WarmGold
import com.bibletriviaquiz.playandwin.ui.theme.WarmGoldContainer
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.platform.LocalContext
import com.bibletriviaquiz.playandwin.data.repository.DefaultBibleQuestions
import com.bibletriviaquiz.playandwin.model.Question
import com.bibletriviaquiz.playandwin.util.ExcelQuestionParser
import com.bibletriviaquiz.playandwin.util.ParseResult
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    adminRepository: AdminRepository,
    orders: List<Order>,
    products: List<Product>,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Withdrawals", "Users & Progress", "Excel Question Upload", "Add Question", "Orders", "Products")
    val coroutineScope = rememberCoroutineScope()

    val withdrawalsDetailed by adminRepository.observeAllWithdrawalsDetailed().collectAsState(initial = emptyList())
    val allUsers by adminRepository.observeAllUsers().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = WarmGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Admin Console", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RoyalNavy)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> AdminWithdrawalsTab(
                    withdrawals = withdrawalsDetailed,
                    onApprovePayment = { reqId, refNum, notes ->
                        coroutineScope.launch {
                            val res = adminRepository.approvePayment(reqId, refNum, notes)
                            res.onSuccess {
                                snackbarHostState.showSnackbar("Payment approved & marked as processed!")
                            }.onFailure { e ->
                                snackbarHostState.showSnackbar("Failed to approve payment: ${e.message}")
                            }
                        }
                    },
                    onMarkApprovedOnly = { reqId, notes ->
                        coroutineScope.launch {
                            val res = adminRepository.markRequestApproved(reqId, notes)
                            res.onSuccess {
                                snackbarHostState.showSnackbar("Request verified & marked as approved.")
                            }.onFailure { e ->
                                snackbarHostState.showSnackbar("Failed: ${e.message}")
                            }
                        }
                    },
                    onRejectAndRefund = { reqId, reason ->
                        coroutineScope.launch {
                            val res = adminRepository.rejectWithdrawalAndRefund(reqId, reason)
                            res.onSuccess {
                                snackbarHostState.showSnackbar("Withdrawal rejected and points refunded to user wallet.")
                            }.onFailure { e ->
                                snackbarHostState.showSnackbar("Failed to reject: ${e.message}")
                            }
                        }
                    }
                )

                1 -> AdminUsersProgressTab(
                    users = allUsers
                )

                2 -> AdminExcelQuestionsTab(
                    adminRepository = adminRepository,
                    snackbarHostState = snackbarHostState
                )

                3 -> AdminQuestionsTab(
                    onAddQuestion = { text, opts, correctIdx, cat, diff, ref, exp ->
                        coroutineScope.launch {
                            try {
                                adminRepository.addQuestion(text, opts, correctIdx, cat, diff, ref, exp)
                                snackbarHostState.showSnackbar("Bible question added to bank")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed: ${e.message}")
                            }
                        }
                    }
                )

                4 -> AdminOrdersTab(
                    orders = orders,
                    onUpdateStatus = { orderId, status, tracking, courier ->
                        coroutineScope.launch {
                            try {
                                adminRepository.updateOrderStatus(orderId, status, tracking, courier)
                                snackbarHostState.showSnackbar("Order updated to $status")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed: ${e.message}")
                            }
                        }
                    }
                )

                5 -> AdminProductsTab(
                    products = products,
                    onSaveProduct = { id, name, desc, cat, price, stock, img ->
                        coroutineScope.launch {
                            try {
                                adminRepository.addOrUpdateProduct(id, name, desc, cat, price, stock, img)
                                snackbarHostState.showSnackbar("Product saved successfully")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed: ${e.message}")
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Enhanced Withdrawals Tab:
 * Displays all users' withdrawal requests, user account details, study progress,
 * referral bonus points, and provides 1-tap payment approval with reference recording.
 */
@Composable
private fun AdminWithdrawalsTab(
    withdrawals: List<AdminWithdrawalDetail>,
    onApprovePayment: (requestId: String, refNum: String, notes: String?) -> Unit,
    onMarkApprovedOnly: (requestId: String, notes: String?) -> Unit,
    onRejectAndRefund: (requestId: String, reason: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") } // "all", "pending", "approved", "processed", "rejected"

    var selectedForApproveDialog by remember { mutableStateOf<AdminWithdrawalDetail?>(null) }
    var selectedForRejectDialog by remember { mutableStateOf<AdminWithdrawalDetail?>(null) }

    val filteredList = remember(withdrawals, searchQuery, selectedFilter) {
        withdrawals.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "all" -> true
                else -> item.request.status == selectedFilter
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.lowercase().trim()
                item.request.requestId.lowercase().contains(q) ||
                item.request.accountName.lowercase().contains(q) ||
                item.request.accountNumber.lowercase().contains(q) ||
                item.request.bankName.lowercase().contains(q) ||
                (item.userProfile?.email?.lowercase()?.contains(q) == true) ||
                (item.userProfile?.displayName?.lowercase()?.contains(q) == true)
            }
            matchesFilter && matchesSearch
        }
    }

    // Stats calculations
    val pendingAmount = withdrawals.filter { it.request.status == "pending" }.sumOf { it.request.usdAmount }
    val pendingPoints = withdrawals.filter { it.request.status == "pending" }.sumOf { it.request.pointsRedeemed }
    val paidAmount = withdrawals.filter { it.request.status == "processed" }.sumOf { it.request.usdAmount }
    val pendingCount = withdrawals.count { it.request.status == "pending" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Metrics Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Withdrawal Requests & Payout Overview",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalNavy
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Pending Payouts", fontSize = 11.sp, color = GoldenBronze)
                            Text(text = "$$pendingAmount USD", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldenBronze)
                            Text(text = "%,d pts ($pendingCount requests)".format(pendingPoints), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Completed Payments", fontSize = 11.sp, color = EmeraldGreen)
                            Text(text = "$$paidAmount USD", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            Text(text = "Total paid out", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Search & Filter Row
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_search_withdrawals"),
                placeholder = { Text("Search by user, email, bank, account #...", fontSize = 13.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = RoyalBlue) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf(
                    "all" to "All (${withdrawals.size})",
                    "pending" to "Pending (${withdrawals.count { it.request.status == "pending" }})",
                    "approved" to "Approved (${withdrawals.count { it.request.status == "approved" }})",
                    "processed" to "Paid (${withdrawals.count { it.request.status == "processed" }})",
                    "rejected" to "Rejected (${withdrawals.count { it.request.status == "rejected" }})"
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedFilter == key) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching withdrawal requests found." else "No withdrawal requests submitted yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredList) { item ->
                WithdrawalDetailedCard(
                    detail = item,
                    onApproveClick = { selectedForApproveDialog = item },
                    onMarkApprovedOnlyClick = { onMarkApprovedOnly(item.request.requestId, null) },
                    onRejectClick = { selectedForRejectDialog = item }
                )
            }
        }
    }

    // Payment Approval Dialog
    if (selectedForApproveDialog != null) {
        val detail = selectedForApproveDialog!!
        val req = detail.request
        var paymentRef by remember { mutableStateOf(req.paymentReference ?: "") }
        var notes by remember { mutableStateOf(req.adminNotes ?: "") }

        AlertDialog(
            onDismissRequest = { selectedForApproveDialog = null },
            icon = {
                Icon(imageVector = Icons.Default.Paid, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Approve & Mark Payout as Paid", fontWeight = FontWeight.Bold, color = RoyalNavy)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "You are approving payment of $${req.usdAmount} USD to:",
                        fontSize = 13.sp
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RoyalBlueContainer)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Holder: ${req.accountName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "Number: ${req.accountNumber}", fontSize = 12.sp)
                            Text(text = "Bank: ${req.bankName} (${req.payoutMethod})", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = paymentRef,
                        onValueChange = { paymentRef = it },
                        label = { Text("Bank Transfer Ref / Tx Confirmation # *") },
                        placeholder = { Text("e.g. WIRE-84729183 / PAYPAL-992") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Admin Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val refToSave = paymentRef.ifBlank { "PROCESSED-${System.currentTimeMillis().toString().takeLast(6)}" }
                        onApprovePayment(req.requestId, refToSave, notes.ifBlank { null })
                        selectedForApproveDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White)
                ) {
                    Text("Confirm Payment", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedForApproveDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rejection Dialog
    if (selectedForRejectDialog != null) {
        val detail = selectedForRejectDialog!!
        val req = detail.request
        var reason by remember { mutableStateOf("Account details could not be verified") }

        AlertDialog(
            onDismissRequest = { selectedForRejectDialog = null },
            icon = {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = QuizRed, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Reject Request & Refund Points", fontWeight = FontWeight.Bold, color = QuizRed)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Rejecting this request will immediately refund %,d points back to user's wallet.".format(req.pointsRedeemed),
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Rejection Reason *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectAndRefund(req.requestId, reason)
                        selectedForRejectDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QuizRed, contentColor = Color.White)
                ) {
                    Text("Reject & Refund Points", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedForRejectDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WithdrawalDetailedCard(
    detail: AdminWithdrawalDetail,
    onApproveClick: () -> Unit,
    onMarkApprovedOnlyClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    val req = detail.request
    val user = detail.userProfile
    val statusColor = when (req.status) {
        "processed" -> EmeraldGreen
        "approved" -> RoyalBlue
        "rejected" -> QuizRed
        else -> GoldenBronze
    }
    val dateStr = req.createdAt?.toDate()?.let {
        SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(it)
    } ?: "Recent"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_withdrawal_${req.requestId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Request ID, Date, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Request #${req.requestId.takeLast(8)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = RoyalNavy
                    )
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = req.status.uppercase(),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Amount Row
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$${req.usdAmount}.00 USD",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${"%,d".format(req.pointsRedeemed)} pts)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 1: User Account & Bank Details
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = RoyalBlueContainer.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Recipient Payout Account Details", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalNavy)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Account Holder: ${req.accountName}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RoyalNavy)
                    Text(text = "Account / IBAN: ${req.accountNumber}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RoyalNavy)
                    Text(text = "Bank / Service: ${req.bankName} • ${req.payoutMethod}", fontSize = 12.sp, color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "User: ${user?.displayName ?: "User"} (${user?.email ?: req.userId})", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section 2: User Progress & Bonus Points Overview
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = WarmGoldContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = GoldenBronze, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "User Activity, Progress & Bonus Points", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalNavy)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Days Active check
                        Column {
                            Text(text = "Account Age:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${detail.daysActive} days",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (detail.isEligible60Days) EmeraldGreen else GoldenBronze
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                if (detail.isEligible60Days) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Eligible", tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                } else {
                                    Text(text = "(60d rule)", fontSize = 10.sp, color = GoldenBronze)
                                }
                            }
                        }

                        // Quiz Progress
                        Column {
                            Text(text = "Quiz Progress:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${user?.totalQuizzesCompleted ?: 0} taken • ${user?.totalQuizzesPassed ?: 0} passed (${detail.passRatePercent}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalNavy
                            )
                        }

                        // Bonus Points from Referrals
                        Column {
                            Text(text = "Referral Bonus:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "+${detail.bonusPointsEarned} pts",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldenBronze
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Current Wallet Remaining Balance: %,d pts".format(user?.currentPoints ?: 0L),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RoyalBlue
                    )
                }
            }

            // Existing Payment Reference or Notes
            if (!req.paymentReference.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Ref: ${req.paymentReference}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                }
            }

            // Admin Action Buttons
            Spacer(modifier = Modifier.height(14.dp))
            when (req.status) {
                "pending" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onApproveClick,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                                .testTag("approve_pay_${req.requestId}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Approve & Pay", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onMarkApprovedOnlyClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Verify", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onRejectClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("reject_${req.requestId}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = QuizRed)
                        ) {
                            Text(text = "Reject", fontSize = 12.sp)
                        }
                    }
                }

                "approved" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onApproveClick,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Mark Paid with Ref #", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onRejectClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = QuizRed)
                        ) {
                            Text(text = "Reject", fontSize = 12.sp)
                        }
                    }
                }

                "processed" -> {
                    Text(
                        text = "✓ Payout fulfilled and processed to beneficiary bank account.",
                        fontSize = 12.sp,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Medium
                    )
                }

                "rejected" -> {
                    Text(
                        text = "✕ Request was rejected. ${"%,d".format(req.pointsRedeemed)} points were refunded to user.",
                        fontSize = 12.sp,
                        color = QuizRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Users & Progress Directory Tab:
 * Displays all registered users, their quiz progress, bonus points from referrals,
 * account age, and points balances.
 */
@Composable
private fun AdminUsersProgressTab(
    users: List<UserProfile>
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(users, query) {
        if (query.isBlank()) users else {
            val q = query.lowercase().trim()
            users.filter { it.displayName.lowercase().contains(q) || it.email.lowercase().contains(q) || it.userId.lowercase().contains(q) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search users by name, email, or UID...", fontSize = 13.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = RoyalBlue) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Text(
                text = "Registered Scholars (${filtered.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = RoyalNavy
            )
        }

        if (filtered.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No users found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(filtered) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.displayName.ifBlank { "Bible Scholar" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = RoyalNavy
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(14.dp))
                                }
                                Text(text = user.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Box(
                                modifier = Modifier
                                    .background(WarmGoldContainer, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "%,d pts".format(user.currentPoints),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = GoldenBronze
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Quizzes Completed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "${user.totalQuizzesCompleted}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(text = "Passed (>=18)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "${user.totalQuizzesPassed}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            }
                            Column {
                                Text(text = "Referrals Count", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "${user.totalReferrals}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
                            }
                            Column {
                                Text(text = "Bonus Earned", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "+${user.successfulReferrals * 100L} pts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldenBronze)
                            }
                        }

                        if (user.referralCode.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Referral Code: ${user.referralCode}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Existing Orders Tab
@Composable
private fun AdminOrdersTab(
    orders: List<Order>,
    onUpdateStatus: (orderId: String, status: String, tracking: String?, courier: String?) -> Unit
) {
    var selectedOrderForUpdate by remember { mutableStateOf<Order?>(null) }

    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No product redemption orders placed yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(orders) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Order #${order.orderId.takeLast(8)}", fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .background(RoyalBlueContainer, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = order.status.replaceFirstChar { it.uppercase() },
                                    color = RoyalBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = order.productName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(text = "Points: ${"%,d".format(order.pointsSpent)}", fontSize = 13.sp, color = GoldenBronze)
                        Text(text = "User ID: ${order.userId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        if (!order.trackingReference.isNullOrBlank()) {
                            Text(text = "Tracking: ${order.courierName ?: "Courier"} - ${order.trackingReference}", fontSize = 12.sp, color = EmeraldGreen)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { selectedOrderForUpdate = order },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Update Order Status & Tracking")
                        }
                    }
                }
            }
        }
    }

    if (selectedOrderForUpdate != null) {
        val order = selectedOrderForUpdate!!
        var status by remember { mutableStateOf(order.status) }
        var tracking by remember { mutableStateOf(order.trackingReference ?: "") }
        var courier by remember { mutableStateOf(order.courierName ?: "") }

        AlertDialog(
            onDismissRequest = { selectedOrderForUpdate = null },
            title = { Text("Update Order #${order.orderId.takeLast(8)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Status:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("pending", "processing", "dispatched", "delivered").forEach { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = courier,
                        onValueChange = { courier = it },
                        label = { Text("Courier Service (e.g. DHL, FedEx)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tracking,
                        onValueChange = { tracking = it },
                        label = { Text("Tracking Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateStatus(order.orderId, status, tracking.ifBlank { null }, courier.ifBlank { null })
                        selectedOrderForUpdate = null
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedOrderForUpdate = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Existing Products Tab
@Composable
private fun AdminProductsTab(
    products: List<Product>,
    onSaveProduct: (id: String?, name: String, desc: String, cat: String, price: Long, stock: Long, img: String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var prodName by remember { mutableStateOf("") }
    var prodDesc by remember { mutableStateOf("") }
    var prodCat by remember { mutableStateOf("Bibles") }
    var prodPrice by remember { mutableStateOf("15000") }
    var prodStock by remember { mutableStateOf("25") }
    var prodImg by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Product Catalog (${products.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Button(
                onClick = {
                    prodName = ""
                    prodDesc = ""
                    prodCat = "Bibles"
                    prodPrice = "15000"
                    prodStock = "25"
                    prodImg = ""
                    showDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = RoyalNavy)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Product")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(products) { prod ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${"%,d".format(prod.pointsPrice)} pts • Stock: ${prod.stockQuantity}", fontSize = 12.sp, color = GoldenBronze)
                            Text("Category: ${prod.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Physical Reward Product") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = prodName, onValueChange = { prodName = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = prodDesc, onValueChange = { prodDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = prodCat, onValueChange = { prodCat = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = prodPrice, onValueChange = { prodPrice = it }, label = { Text("Points Price (min 10,000)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = prodStock, onValueChange = { prodStock = it }, label = { Text("Stock Quantity") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = prodImg, onValueChange = { prodImg = it }, label = { Text("Image URL") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = prodPrice.toLongOrNull() ?: 10000L
                        val stock = prodStock.toLongOrNull() ?: 10L
                        onSaveProduct(null, prodName, prodDesc, prodCat, price.coerceAtLeast(10000L), stock, prodImg)
                        showDialog = false
                    }
                ) {
                    Text("Save Product")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// Existing Questions Tab
@Composable
private fun AdminQuestionsTab(
    onAddQuestion: (text: String, options: List<String>, correctIdx: Int, cat: String, diff: String, ref: String?, exp: String?) -> Unit
) {
    var qText by remember { mutableStateOf("") }
    var opt0 by remember { mutableStateOf("") }
    var opt1 by remember { mutableStateOf("") }
    var opt2 by remember { mutableStateOf("") }
    var opt3 by remember { mutableStateOf("") }
    var correctIdx by remember { mutableIntStateOf(0) }
    var category by remember { mutableStateOf("Old Testament") }
    var reference by remember { mutableStateOf("") }
    var explanation by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Add New Question to Question Bank", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        OutlinedTextField(value = qText, onValueChange = { qText = it }, label = { Text("Question Text") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = opt0, onValueChange = { opt0 = it }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = opt1, onValueChange = { opt1 = it }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = opt2, onValueChange = { opt2 = it }, label = { Text("Option C") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = opt3, onValueChange = { opt3 = it }, label = { Text("Option D") }, modifier = Modifier.fillMaxWidth())

        Text("Select Correct Option:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("A", "B", "C", "D").forEachIndexed { idx, label ->
                FilterChip(
                    selected = correctIdx == idx,
                    onClick = { correctIdx = idx },
                    label = { Text(label) }
                )
            }
        }

        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = reference, onValueChange = { reference = it }, label = { Text("Bible Reference (e.g. Genesis 1:1)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = explanation, onValueChange = { explanation = it }, label = { Text("Explanation") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                if (qText.isNotBlank() && opt0.isNotBlank() && opt1.isNotBlank()) {
                    onAddQuestion(qText, listOf(opt0, opt1, opt2.ifBlank { "None of the above" }, opt3.ifBlank { "All of the above" }), correctIdx, category, "Medium", reference.ifBlank { null }, explanation.ifBlank { null })
                    qText = ""
                    opt0 = ""
                    opt1 = ""
                    opt2 = ""
                    opt3 = ""
                    reference = ""
                    explanation = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
        ) {
            Text("Publish Question")
        }
    }
}

/**
 * Excel Question Upload Tab:
 * Allows administrators to upload questions and answers via Excel spreadsheet (.xlsx, .csv, .tsv)
 * or paste rows directly into the question bank for each question category.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminExcelQuestionsTab(
    adminRepository: AdminRepository,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allFirestoreQuestions by adminRepository.observeAllQuestions().collectAsState(initial = emptyList())

    val targetCategories = remember {
        listOf("Auto-detect from Sheet") + DefaultBibleQuestions.categories
    }
    var selectedTargetCategory by remember { mutableStateOf(targetCategories.first()) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var parseResult by remember { mutableStateOf<ParseResult?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var pastedText by remember { mutableStateOf("") }
    var showPasteSection by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }

    // Category filter for viewing questions in the database
    var selectedCategoryFilter by remember { mutableStateOf<String?>("All") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileName = uri.lastPathSegment ?: "Selected File"
            val categoryOverride = if (selectedTargetCategory == "Auto-detect from Sheet") null else selectedTargetCategory
            val result = ExcelQuestionParser.parseFromUri(context, uri, categoryOverride)
            parseResult = result
            if (result.questions.isNotEmpty()) {
                uploadStatusMessage = "Successfully parsed ${result.questions.size} questions from file!"
            } else {
                uploadStatusMessage = "No valid questions found in file."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero / Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RoyalNavy),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = WarmGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Excel Question Bank Importer",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Import questions into question bank categories via Excel (.xlsx) or CSV",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Target Category Selector
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Target Category for Uploaded Questions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue
                )
                Text(
                    text = "Choose 'Auto-detect' to let the sheet's Category column route questions into their respective Bible categories, or designate a specific category for all rows.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedTargetCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Designated Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        targetCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, fontWeight = if (cat == selectedTargetCategory) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedTargetCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Import Actions Card: File Picker & Paste Area & Template Download
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "2. Select Excel Sheet or Paste Data",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pick File Button
                    Button(
                        onClick = {
                            filePickerLauncher.launch("*/*")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_upload_excel_file_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Excel File")
                    }

                    // Copy Template Button
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Bible Questions Template", ExcelQuestionParser.getSampleCsvTemplate())
                            clipboard.setPrimaryClip(clip)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Sample Excel/CSV template copied to clipboard!")
                            }
                        },
                        modifier = Modifier.testTag("admin_copy_template_button")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Template")
                    }
                }

                if (selectedFileName != null) {
                    Text(
                        text = "Selected file: $selectedFileName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldGreen
                    )
                }

                // Toggle Paste Area
                OutlinedButton(
                    onClick = { showPasteSection = !showPasteSection },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (showPasteSection) Icons.Default.Close else Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showPasteSection) "Hide Paste Rows Box" else "Or Paste Spreadsheet Rows Directly")
                }

                if (showPasteSection) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Paste rows copied from Excel, Google Sheets, or CSV (including header or tab-separated):",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = pastedText,
                            onValueChange = { pastedText = it },
                            label = { Text("Paste Spreadsheet Content Here") },
                            placeholder = { Text("Category\tQuestion Text\tOption A\tOption B\tOption C\tOption D\tCorrect Option\tBible Reference\tExplanation") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .testTag("admin_paste_excel_input"),
                            maxLines = 10
                        )
                        Button(
                            onClick = {
                                if (pastedText.isNotBlank()) {
                                    val catOverride = if (selectedTargetCategory == "Auto-detect from Sheet") null else selectedTargetCategory
                                    val res = ExcelQuestionParser.parseDelimitedText(pastedText, catOverride)
                                    parseResult = res
                                    if (res.questions.isNotEmpty()) {
                                        uploadStatusMessage = "Parsed ${res.questions.size} questions from pasted text!"
                                    } else {
                                        uploadStatusMessage = "No valid questions parsed from pasted text."
                                    }
                                }
                            },
                            enabled = pastedText.isNotBlank(),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Parse Pasted Data")
                        }
                    }
                }
            }
        }

        // Supported Columns Format Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WarmGoldContainer.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = GoldenBronze, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Supported Excel Columns (Any Order):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldenBronze)
                }
                Text(
                    text = "• Category (e.g. Old Testament, Miracles of Jesus, Women in the Bible...)\n" +
                            "• Question / Question Text (at least 5 characters)\n" +
                            "• Option A, Option B, Option C, Option D (4 options)\n" +
                            "• Correct Option (A, B, C, D or 1, 2, 3, 4 or full matching answer text)\n" +
                            "• Bible Reference (optional, e.g. Genesis 1:1, John 3:16)\n" +
                            "• Explanation (optional, context for user study)\n" +
                            "• Difficulty (optional: Easy, Medium, Hard)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Parse Results & Upload Action
        parseResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.questions.isNotEmpty()) EmeraldGreenContainer.copy(alpha = 0.3f) else QuizRedContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Parsed Preview: ${result.questions.size} Valid Questions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (result.questions.isNotEmpty()) EmeraldGreen else QuizRed
                        )
                        Text(
                            text = "${result.totalRowsProcessed} rows scanned",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Category Breakdown Chips
                    if (result.categoryCounts.isNotEmpty()) {
                        Text("Category Breakdown:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(result.categoryCounts.toList()) { (cat, count) ->
                                    FilterChip(
                                        selected = true,
                                        onClick = {},
                                        label = { Text("$cat ($count)") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = RoyalBlueContainer,
                                            selectedLabelColor = RoyalBlue
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Errors / Warnings if any
                    if (result.errors.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = QuizRedContainer.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = QuizRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${result.errors.size} Warnings / Skipped Rows:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = QuizRed)
                                }
                                result.errors.take(4).forEach { err ->
                                    Text("• $err", fontSize = 10.sp, color = QuizRed)
                                }
                                if (result.errors.size > 4) {
                                    Text("...and ${result.errors.size - 4} more warnings", fontSize = 10.sp, color = QuizRed)
                                }
                            }
                        }
                    }

                    // Sample question previews (up to 3)
                    if (result.questions.isNotEmpty()) {
                        Text("Preview First Few Questions:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        result.questions.take(3).forEachIndexed { idx, q ->
                            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("${idx + 1}. [${q.category}]", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
                                        q.bibleReference?.let {
                                            Text(it, fontSize = 10.sp, color = GoldenBronze)
                                        }
                                    }
                                    Text(q.questionText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    q.options.forEachIndexed { oIdx, opt ->
                                        val isCorrect = oIdx.toLong() == q.correctOptionIndex
                                        Text(
                                            text = "${('A' + oIdx)}: $opt ${if (isCorrect) "✓ (Correct)" else ""}",
                                            fontSize = 11.sp,
                                            color = if (isCorrect) EmeraldGreen else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Upload Button
                        Button(
                            onClick = {
                                isUploading = true
                                coroutineScope.launch {
                                    val uploadRes = adminRepository.batchUploadQuestions(result.questions)
                                    isUploading = false
                                    uploadRes.onSuccess { count ->
                                        snackbarHostState.showSnackbar("Successfully saved $count questions into Cloud Firestore Question Bank!")
                                        parseResult = null
                                        selectedFileName = null
                                        pastedText = ""
                                        uploadStatusMessage = "All $count questions successfully uploaded!"
                                    }.onFailure { err ->
                                        snackbarHostState.showSnackbar("Upload failed: ${err.message}")
                                        uploadStatusMessage = "Upload failed: ${err.message}"
                                    }
                                }
                            },
                            enabled = !isUploading && result.questions.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("admin_confirm_batch_upload_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading to Firestore Bank...", color = Color.White)
                            } else {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload ${result.questions.size} Questions to Question Bank", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        uploadStatusMessage?.let { msg ->
            Text(
                text = msg,
                fontSize = 12.sp,
                color = RoyalBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Question Bank Stats & Category Browser
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LibraryBooks, contentDescription = null, tint = RoyalBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Live Question Bank", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text(
                        "100,000+ / Category Active",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        fontSize = 12.sp
                    )
                }

                // Bank capacity information banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmGoldContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Question Bank Scale: 100,000+ questions per category (1.5M+ total pool). Questions are guaranteed not to repeat within 10 days for any user. Custom Excel/CSV uploads augment the cloud bank.",
                            fontSize = 11.sp,
                            color = RoyalNavy,
                            lineHeight = 15.sp
                        )
                    }
                }

                Text(
                    text = "Filter uploaded questions by category (Cloud uploads: ${allFirestoreQuestions.size}):",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Category Filter Chips
                val filterOptions = listOf("All") + DefaultBibleQuestions.categories
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filterOptions) { cat ->
                        val count = if (cat == "All") {
                            allFirestoreQuestions.size
                        } else {
                            allFirestoreQuestions.count { it.category.equals(cat, ignoreCase = true) }
                        }
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text("$cat ($count)") }
                        )
                    }
                }

                val displayedQuestions = if (selectedCategoryFilter == "All") {
                    allFirestoreQuestions
                } else {
                    allFirestoreQuestions.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
                }

                if (displayedQuestions.isEmpty()) {
                    Text(
                        text = if (allFirestoreQuestions.isEmpty()) "No custom questions uploaded to Cloud Firestore yet. Use the Excel/CSV uploader above to populate categories." else "No questions in this category yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Text(
                        text = "Showing ${displayedQuestions.take(15).size} of ${displayedQuestions.size} uploaded questions:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    displayedQuestions.take(15).forEach { q ->
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(q.category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
                                    q.bibleReference?.let {
                                        Text(it, fontSize = 10.sp, color = GoldenBronze)
                                    }
                                }
                                Text(q.questionText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val correctOpt = q.options.getOrNull(q.correctOptionIndex.toInt()) ?: ""
                                    Text("Ans: $correctOpt", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                                    Text("Diff: ${q.difficulty}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

