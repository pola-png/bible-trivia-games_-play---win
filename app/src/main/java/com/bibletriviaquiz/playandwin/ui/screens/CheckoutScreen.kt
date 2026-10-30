package com.bibletriviaquiz.playandwin.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibletriviaquiz.playandwin.model.DeliveryAddress
import com.bibletriviaquiz.playandwin.model.Product
import com.bibletriviaquiz.playandwin.ui.theme.EmeraldGreen
import com.bibletriviaquiz.playandwin.ui.theme.GoldenBronze
import com.bibletriviaquiz.playandwin.ui.theme.RoyalBlue
import com.bibletriviaquiz.playandwin.ui.theme.RoyalNavy
import com.bibletriviaquiz.playandwin.ui.theme.WarmGold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    product: Product,
    userPoints: Long,
    addresses: List<DeliveryAddress>,
    onSaveNewAddress: suspend (recipientName: String, phone: String, state: String, city: String, street: String, landmark: String?) -> DeliveryAddress?,
    onConfirmRedemption: suspend (DeliveryAddress) -> Result<Unit>,
    onSuccess: () -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var selectedAddressId by remember { mutableStateOf(addresses.firstOrNull()?.addressId) }
    var showNewAddressForm by remember { mutableStateOf(addresses.isEmpty()) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // New Address Form Fields
    var recipientName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var stateName by remember { mutableStateOf("Lagos") }
    var cityName by remember { mutableStateOf("") }
    var streetAddress by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val remainingPoints = userPoints - product.pointsPrice
    val selectedAddress = addresses.firstOrNull { it.addressId == selectedAddressId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm Redemption", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("checkout_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("submit_redemption_order_button"),
                        enabled = !isSubmitting && selectedAddress != null,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarmGold,
                            contentColor = RoyalNavy
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = RoyalNavy, strokeWidth = 2.dp)
                        } else {
                            Text("Place Redemption Order", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Product Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Item to Redeem", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = product.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(text = product.category, fontSize = 13.sp, color = RoyalBlue)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Balance:", fontSize = 13.sp, color = Color(0xFF64748B))
                        Text("%,d pts".format(userPoints), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Points Required:", fontSize = 13.sp, color = Color(0xFF64748B))
                        Text("-%,d pts".format(product.pointsPrice), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Remaining Balance:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("%,d pts".format(remainingPoints), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldGreen)
                    }
                }
            }

            // Delivery Address Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Delivery Address (Nigeria)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (!showNewAddressForm) {
                    OutlinedButton(
                        onClick = { showNewAddressForm = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New", fontSize = 12.sp)
                    }
                }
            }

            if (showNewAddressForm) {
                // New Address Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Enter Shipping Address", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = recipientName,
                            onValueChange = { recipientName = it },
                            label = { Text("Recipient Full Name") },
                            modifier = Modifier.fillMaxWidth().testTag("address_name_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number (e.g., 08012345678)") },
                            modifier = Modifier.fillMaxWidth().testTag("address_phone_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = stateName,
                            onValueChange = { stateName = it },
                            label = { Text("State (e.g., Lagos, Abuja FCT, Rivers)") },
                            modifier = Modifier.fillMaxWidth().testTag("address_state_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = cityName,
                            onValueChange = { cityName = it },
                            label = { Text("City / Town / LGA") },
                            modifier = Modifier.fillMaxWidth().testTag("address_city_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = streetAddress,
                            onValueChange = { streetAddress = it },
                            label = { Text("Street Address") },
                            modifier = Modifier.fillMaxWidth().testTag("address_street_input"),
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = landmark,
                            onValueChange = { landmark = it },
                            label = { Text("Nearest Landmark / Area (Optional)") },
                            modifier = Modifier.fillMaxWidth().testTag("address_landmark_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (recipientName.isBlank() || phoneNumber.isBlank() || streetAddress.isBlank() || cityName.isBlank()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Please fill in recipient name, phone, city, and street address.")
                                    }
                                    return@Button
                                }
                                coroutineScope.launch {
                                    val saved = onSaveNewAddress(recipientName, phoneNumber, stateName, cityName, streetAddress, landmark.ifBlank { null })
                                    if (saved != null) {
                                        selectedAddressId = saved.addressId
                                        showNewAddressForm = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_address_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                        ) {
                            Text("Save Address")
                        }
                    }
                }
            } else {
                // List Saved Addresses
                addresses.forEach { addr ->
                    val isSelected = addr.addressId == selectedAddressId
                    OutlinedCard(
                        onClick = { selectedAddressId = addr.addressId },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) RoyalBlue else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .testTag("address_card_${addr.addressId}"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(if (isSelected) RoyalBlue else Color.Transparent, CircleShape)
                                    .border(1.dp, if (isSelected) RoyalBlue else Color(0xFF94A3B8), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = addr.recipientName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = addr.phoneNumber, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "${addr.streetAddress}, ${addr.city}, ${addr.state}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!addr.landmark.isNullOrBlank()) {
                                    Text(text = "Near: ${addr.landmark}", fontSize = 11.sp, color = GoldenBronze)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog && selectedAddress != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Product Redemption") },
            text = {
                Column {
                    Text("Are you sure you want to redeem ${product.name}?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• %,d points will be deducted from your wallet.".format(product.pointsPrice))
                    Text("• Deliver to: ${selectedAddress.recipientName}, ${selectedAddress.streetAddress}, ${selectedAddress.city}, ${selectedAddress.state}.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Points are non-refundable once shipped.", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isSubmitting = true
                        coroutineScope.launch {
                            val result = onConfirmRedemption(selectedAddress)
                            isSubmitting = false
                            if (result.isSuccess) {
                                onSuccess()
                            } else {
                                snackbarHostState.showSnackbar(result.exceptionOrNull()?.localizedMessage ?: "Redemption failed")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = RoyalNavy),
                    modifier = Modifier.testTag("dialog_confirm_redemption_button")
                ) {
                    Text("Confirm Redemption")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
