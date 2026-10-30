package com.bibletriviaquiz.playandwin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.content.Intent
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import kotlinx.coroutines.tasks.await
import com.bibletriviaquiz.playandwin.data.repository.AddressRepository
import com.bibletriviaquiz.playandwin.data.repository.AdminRepository
import com.bibletriviaquiz.playandwin.data.repository.QuizRepository
import com.bibletriviaquiz.playandwin.data.repository.ReferralRepository
import com.bibletriviaquiz.playandwin.data.repository.ShopRepository
import com.bibletriviaquiz.playandwin.data.repository.WalletRepository
import com.bibletriviaquiz.playandwin.model.DeliveryAddress
import com.bibletriviaquiz.playandwin.model.Product
import com.bibletriviaquiz.playandwin.ui.screens.CashOutScreen
import com.bibletriviaquiz.playandwin.ui.screens.AdminScreen
import com.bibletriviaquiz.playandwin.ui.screens.AuthScreen
import com.bibletriviaquiz.playandwin.ui.screens.CheckoutScreen
import com.bibletriviaquiz.playandwin.ui.screens.HomeScreen
import com.bibletriviaquiz.playandwin.ui.screens.OrderHistoryScreen
import com.bibletriviaquiz.playandwin.ui.screens.ProductDetailScreen
import com.bibletriviaquiz.playandwin.ui.screens.QuizCategoryScreen
import com.bibletriviaquiz.playandwin.ui.screens.QuizResultScreen
import com.bibletriviaquiz.playandwin.ui.screens.QuizScreen
import com.bibletriviaquiz.playandwin.ui.screens.ReferralScreen
import com.bibletriviaquiz.playandwin.ui.screens.SettingsScreen
import com.bibletriviaquiz.playandwin.ui.screens.ShopScreen
import com.bibletriviaquiz.playandwin.ui.screens.WalletScreen
import com.bibletriviaquiz.playandwin.ui.theme.BibleQuizRewardsTheme
import com.bibletriviaquiz.playandwin.ui.theme.BibleTriviaQuizTheme
import com.bibletriviaquiz.playandwin.ui.theme.RoyalBlue
import com.bibletriviaquiz.playandwin.ui.theme.RoyalNavy
import com.bibletriviaquiz.playandwin.ui.theme.WarmGold
import com.bibletriviaquiz.playandwin.util.CategoryCycleManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.games.PlayGamesSdk
import com.bibletriviaquiz.playandwin.ads.AppOpenAdManager
import com.bibletriviaquiz.playandwin.ads.RewardedAdManager
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    CATEGORIES,
    QUIZ,
    RESULT,
    SHOP,
    PRODUCT_DETAIL,
    CHECKOUT,
    ORDERS,
    WALLET,
    CASHOUT,
    REFERRAL,
    ADMIN,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    private lateinit var appOpenAdManager: AppOpenAdManager
    private lateinit var rewardedAdManager: RewardedAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Google Mobile Ads SDK
        MobileAds.initialize(this) {}

        // Initialize Google Play Games Services SDK v2
        PlayGamesSdk.initialize(this)

        appOpenAdManager = AppOpenAdManager(applicationContext)
        appOpenAdManager.loadAd {
            appOpenAdManager.showAdIfAvailable(this)
        }

        rewardedAdManager = RewardedAdManager(applicationContext)
        rewardedAdManager.loadAd()

        val initialReferralCode = extractReferralCodeFromIntent(intent)
        setContent {
            BibleTriviaQuizTheme {
                MainApp(
                    initialReferralCode = initialReferralCode,
                    rewardedAdManager = rewardedAdManager,
                    activity = this
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::appOpenAdManager.isInitialized) {
            appOpenAdManager.showAdIfAvailable(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun extractReferralCodeFromIntent(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return data.getQueryParameter("code")
    }
}

@Composable
fun MainApp(
    initialReferralCode: String? = null,
    rewardedAdManager: RewardedAdManager? = null,
    activity: android.app.Activity? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val credentialManager = remember { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            },
            snackbarHostState = snackbarHostState
        )
    } else {
        val uid = currentUser!!.uid
        val quizRepo = remember(uid) { QuizRepository(context) }
        val walletRepo = remember(uid) { WalletRepository(context) }
        val shopRepo = remember(uid) { ShopRepository(context) }
        val addressRepo = remember(uid) { AddressRepository(context) }
        val adminRepo = remember(uid) { AdminRepository(context) }
        val referralRepo = remember(uid) { ReferralRepository(context) }

        // Automatically initialize user profile document in Firestore and referral system
        LaunchedEffect(uid) {
            try {
                quizRepo.createOrUpdateUserProfile(
                    email = currentUser?.email ?: "",
                    displayName = currentUser?.displayName ?: "Bible Scholar"
                )
                referralRepo.ensureReferralCodeRegistered(
                    displayName = currentUser?.displayName ?: "Bible Scholar"
                )
                if (!initialReferralCode.isNullOrBlank()) {
                    referralRepo.applyReferralCode(initialReferralCode)
                }
            } catch (_: Exception) {}
        }

        // Live States
        val userProfile by quizRepo.observeUserProfile().collectAsState(initial = null)
        val transactions by walletRepo.observeTransactions().collectAsState(initial = emptyList())
        val cashRequests by walletRepo.observeCashRedemptionRequests().collectAsState(initial = emptyList())
        val products by shopRepo.observeProducts().collectAsState(initial = emptyList())
        val orders by shopRepo.observeOrders().collectAsState(initial = emptyList())
        val liveQuestions by quizRepo.observeAllQuestions().collectAsState(initial = emptyList())
        val addresses by addressRepo.observeAddresses().collectAsState(initial = emptyList())

        val isAdmin = adminRepo.isCurrentUserAdmin()
        val daysActive = remember(userProfile?.createdAt) {
            walletRepo.calculateDaysActive(userProfile?.createdAt)
        }

        // Navigation State
        var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
        var selectedCategory by remember { mutableStateOf("Old Testament") }
        var lastCorrectCount by remember { mutableIntStateOf(0) }
        var lastTimedOutCount by remember { mutableIntStateOf(0) }
        var selectedProduct by remember { mutableStateOf<Product?>(null) }

        // BackHandler for sub-screens
        if (currentScreen != AppScreen.HOME) {
            BackHandler {
                currentScreen = when (currentScreen) {
                    AppScreen.QUIZ -> AppScreen.CATEGORIES
                    AppScreen.RESULT -> AppScreen.HOME
                    AppScreen.CATEGORIES -> AppScreen.HOME
                    AppScreen.PRODUCT_DETAIL -> AppScreen.SHOP
                    AppScreen.CHECKOUT -> AppScreen.PRODUCT_DETAIL
                    AppScreen.ORDERS -> AppScreen.HOME
                    AppScreen.WALLET -> AppScreen.HOME
                    AppScreen.CASHOUT -> AppScreen.WALLET
                    AppScreen.REFERRAL -> AppScreen.HOME
                    AppScreen.ADMIN -> AppScreen.HOME
                    AppScreen.SETTINGS -> AppScreen.HOME
                    else -> AppScreen.HOME
                }
            }
        }

        val showBottomNav = currentScreen != AppScreen.QUIZ && currentScreen != AppScreen.CASHOUT && currentScreen != AppScreen.REFERRAL

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomNav) {
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.HOME,
                            onClick = { currentScreen = AppScreen.HOME },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text("Home", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = RoyalBlue.copy(alpha = 0.15f))
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.SHOP,
                            onClick = { currentScreen = AppScreen.SHOP },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.SHOP) Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag,
                                    contentDescription = "Shop"
                                )
                            },
                            label = { Text("Shop", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = RoyalBlue.copy(alpha = 0.15f))
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.WALLET,
                            onClick = { currentScreen = AppScreen.WALLET },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.WALLET) Icons.Filled.Wallet else Icons.Outlined.Wallet,
                                    contentDescription = "Wallet"
                                )
                            },
                            label = { Text("Wallet", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = RoyalBlue.copy(alpha = 0.15f))
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.SETTINGS,
                            onClick = { currentScreen = AppScreen.SETTINGS },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = RoyalBlue.copy(alpha = 0.15f))
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    AppScreen.HOME -> {
                        val handleWatchRewardedAd: () -> Unit = {
                            if (rewardedAdManager != null && activity != null) {
                                rewardedAdManager.showAd(
                                    activity = activity,
                                    onUserEarnedReward = { rewardAmount ->
                                        coroutineScope.launch {
                                            val result = walletRepo.claimRewardedAdBonus(rewardAmount.toLong())
                                            if (result.isSuccess) {
                                                snackbarHostState.showSnackbar("🎉 Claimed +$rewardAmount free bonus points!")
                                            } else {
                                                snackbarHostState.showSnackbar("Bonus recorded!")
                                            }
                                        }
                                    },
                                    onAdDismissed = {
                                        rewardedAdManager.loadAd()
                                    }
                                )
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Ad is loading, please try again in a moment.")
                                }
                            }
                        }

                        HomeScreen(
                            userProfile = userProfile,
                            daysActive = daysActive,
                            recentTransactions = transactions,
                            featuredProducts = products,
                            isAdmin = isAdmin,
                            onPlayQuizClick = { currentScreen = AppScreen.CATEGORIES },
                            onShopClick = { currentScreen = AppScreen.SHOP },
                            onWalletClick = { currentScreen = AppScreen.WALLET },
                            onCashOutClick = { currentScreen = AppScreen.CASHOUT },
                            onOrdersClick = { currentScreen = AppScreen.ORDERS },
                            onReferralClick = { currentScreen = AppScreen.REFERRAL },
                            onHelpClick = { currentScreen = AppScreen.SETTINGS },
                            onAdminClick = { currentScreen = AppScreen.ADMIN },
                            onWatchRewardedAdClick = handleWatchRewardedAd
                        )
                    }

                    AppScreen.CATEGORIES -> {
                        val currentRound = userProfile?.categoryCycleRound ?: 1L
                        val completedList = userProfile?.completedCategoriesInCycle ?: emptyList()
                        QuizCategoryScreen(
                            userId = uid,
                            currentRound = currentRound,
                            completedCategories = completedList,
                            onCategorySelected = { cat ->
                                selectedCategory = cat
                                currentScreen = AppScreen.QUIZ
                            },
                            onBackClick = { currentScreen = AppScreen.HOME }
                        )
                    }

                    AppScreen.QUIZ -> QuizScreen(
                        category = selectedCategory,
                        questions = liveQuestions,
                        onQuizComplete = { correct, timedOut ->
                            lastCorrectCount = correct
                            lastTimedOutCount = timedOut
                            coroutineScope.launch {
                                try {
                                    val session = quizRepo.saveCompletedQuizSession(selectedCategory, correct, timedOut)
                                    val roundNow = userProfile?.categoryCycleRound ?: 1L
                                    val completedNow = userProfile?.completedCategoriesInCycle ?: emptyList()
                                    if (session.isPassed) {
                                        if (completedNow.size >= CategoryCycleManager.totalCategoryCount) {
                                            snackbarHostState.showSnackbar("🎉 Round $roundNow Complete! All 15 Bible categories completed once.")
                                        } else {
                                            snackbarHostState.showSnackbar("Passed with $correct/30! +50 Points earned & stage cleared!")
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Scored $correct/30. Need 25/30 to earn 50 points and progress to another stage.")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Note: Session scored locally (${e.message})")
                                }
                            }
                            currentScreen = AppScreen.RESULT
                        },
                        onExitQuiz = { currentScreen = AppScreen.CATEGORIES },
                        onShowRewardedAd = { onAdFinished ->
                            if (rewardedAdManager != null && activity != null) {
                                if (rewardedAdManager.isReady()) {
                                    var rewardEarned = false
                                    rewardedAdManager.showAd(
                                        activity = activity,
                                        onUserEarnedReward = { rewardAmount ->
                                            rewardEarned = true
                                            coroutineScope.launch {
                                                walletRepo.claimRewardedAdBonus(rewardAmount.toLong())
                                                snackbarHostState.showSnackbar("🎉 Claimed +$rewardAmount free bonus points!")
                                            }
                                        },
                                        onAdDismissed = {
                                            rewardedAdManager.loadAd()
                                            onAdFinished(rewardEarned)
                                        },
                                        onAdNotReady = {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Ad is still loading. Please try again in a few seconds.")
                                            }
                                            onAdFinished(false)
                                        }
                                    )
                                } else {
                                    rewardedAdManager.loadAd()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Loading rewarded ad... Please tap again in a moment.")
                                    }
                                    onAdFinished(false)
                                }
                            } else {
                                onAdFinished(true)
                            }
                        }
                    )

                    AppScreen.RESULT -> QuizResultScreen(
                        correctCount = lastCorrectCount,
                        timedOutCount = lastTimedOutCount,
                        onPlayAgainClick = { currentScreen = AppScreen.CATEGORIES },
                        onWalletClick = { currentScreen = AppScreen.WALLET },
                        onHomeClick = { currentScreen = AppScreen.HOME }
                    )

                    AppScreen.SHOP -> ShopScreen(
                        products = products,
                        userPoints = userProfile?.currentPoints ?: 0L,
                        onProductClick = { prod ->
                            selectedProduct = prod
                            currentScreen = AppScreen.PRODUCT_DETAIL
                        },
                        onBackClick = { currentScreen = AppScreen.HOME }
                    )

                    AppScreen.PRODUCT_DETAIL -> {
                        if (selectedProduct != null) {
                            ProductDetailScreen(
                                product = selectedProduct!!,
                                userPoints = userProfile?.currentPoints ?: 0L,
                                onProceedToCheckout = { currentScreen = AppScreen.CHECKOUT },
                                onBackClick = { currentScreen = AppScreen.SHOP }
                            )
                        } else {
                            currentScreen = AppScreen.SHOP
                        }
                    }

                    AppScreen.CHECKOUT -> {
                        if (selectedProduct != null) {
                            CheckoutScreen(
                                product = selectedProduct!!,
                                userPoints = userProfile?.currentPoints ?: 0L,
                                addresses = addresses,
                                onSaveNewAddress = { name, phone, state, city, street, landmark ->
                                    try {
                                        addressRepo.saveAddress(name, phone, state, city, street, landmark, null, true)
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Could not save address: ${e.message}")
                                        null
                                    }
                                },
                                onConfirmRedemption = { addr ->
                                    val result = shopRepo.redeemProduct(selectedProduct!!, addr)
                                    if (result.isSuccess) {
                                        Result.success(Unit)
                                    } else {
                                        Result.failure(result.exceptionOrNull() ?: Exception("Redemption failed"))
                                    }
                                },
                                onSuccess = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Order placed successfully!")
                                    }
                                    currentScreen = AppScreen.ORDERS
                                },
                                onBackClick = { currentScreen = AppScreen.PRODUCT_DETAIL },
                                snackbarHostState = snackbarHostState
                            )
                        } else {
                            currentScreen = AppScreen.SHOP
                        }
                    }

                    AppScreen.ORDERS -> OrderHistoryScreen(
                        orders = orders,
                        onShopClick = { currentScreen = AppScreen.SHOP },
                        onBackClick = { currentScreen = AppScreen.HOME }
                    )

                    AppScreen.WALLET -> {
                        val handleWatchRewardedAd: () -> Unit = {
                            if (rewardedAdManager != null && activity != null) {
                                rewardedAdManager.showAd(
                                    activity = activity,
                                    onUserEarnedReward = { rewardAmount ->
                                        coroutineScope.launch {
                                            val result = walletRepo.claimRewardedAdBonus(rewardAmount.toLong())
                                            if (result.isSuccess) {
                                                snackbarHostState.showSnackbar("🎉 Claimed +$rewardAmount free bonus points!")
                                            } else {
                                                snackbarHostState.showSnackbar("Bonus recorded!")
                                            }
                                        }
                                    },
                                    onAdDismissed = {
                                        rewardedAdManager.loadAd()
                                    }
                                )
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Ad is loading, please try again in a moment.")
                                }
                            }
                        }

                        WalletScreen(
                            userProfile = userProfile,
                            daysActive = daysActive,
                            transactions = transactions,
                            onShopClick = { currentScreen = AppScreen.SHOP },
                            onCashOutClick = { currentScreen = AppScreen.CASHOUT },
                            onReferralClick = { currentScreen = AppScreen.REFERRAL },
                            onBackClick = { currentScreen = AppScreen.HOME },
                            onWatchRewardedAdClick = handleWatchRewardedAd
                        )
                    }

                    AppScreen.CASHOUT -> CashOutScreen(
                        userProfile = userProfile,
                        daysActive = daysActive,
                        cashRequests = cashRequests,
                        onRequestCashOut = { points, name, num, bank, method ->
                            walletRepo.requestCashRedemption(points, name, num, bank, method)
                        },
                        onBackClick = { currentScreen = AppScreen.WALLET },
                        snackbarHostState = snackbarHostState
                    )

                    AppScreen.REFERRAL -> ReferralScreen(
                        userProfile = userProfile,
                        referralRepository = referralRepo,
                        onBackClick = { currentScreen = AppScreen.HOME },
                        snackbarHostState = snackbarHostState
                    )

                    AppScreen.ADMIN -> AdminScreen(
                        adminRepository = adminRepo,
                        orders = orders,
                        products = products,
                        onBackClick = { currentScreen = AppScreen.HOME },
                        snackbarHostState = snackbarHostState
                    )

                    AppScreen.SETTINGS -> SettingsScreen(
                        userEmail = currentUser?.email ?: "",
                        onSignOutClick = {
                            Firebase.auth.signOut()
                            coroutineScope.launch {
                                try {
                                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                } catch (_: Exception) {}
                                currentUser = null
                            }
                        },
                        onDeleteAccountClick = {
                            coroutineScope.launch {
                                try {
                                    val user = Firebase.auth.currentUser
                                    if (user != null) {
                                        val uid = user.uid
                                        quizRepo.deleteUserData(uid)
                                        user.delete().await()
                                    }
                                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                    currentUser = null
                                    snackbarHostState.showSnackbar("Account and associated data deleted permanently.")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Recent login required to confirm deletion. Signing out...")
                                    Firebase.auth.signOut()
                                    currentUser = null
                                }
                            }
                        },
                        onReferralClick = { currentScreen = AppScreen.REFERRAL },
                        onBackClick = { currentScreen = AppScreen.HOME }
                    )
                }
            }
        }
    }
}
