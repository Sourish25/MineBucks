package com.Sourish25.MineBucks.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Warning // Added import
import androidx.compose.material.icons.automirrored.filled.ArrowForward // Added import
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff // Added import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import com.Sourish25.MineBucks.ui.theme.ExpressiveMotion
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource


// ... (Existing content)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    viewModel: RevenueViewModel,
    uiState: RevenueUiState,
    onOpenSettings: () -> Unit
) {
    // val scrollState = rememberScrollState() // Unused
    val greeting = remember { viewModel.getGreeting() }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current 
    
    // Refresh data on entry
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = { viewModel.refreshData() }
    )
    
    // --- BACKGROUND SCRAPER ---
    // Invisible WebView to keep data fresh using saved cookies
    val context = androidx.compose.ui.platform.LocalContext.current
    if (uiState.onboarded && !uiState.curseForgeCookies.isNullOrBlank()) {
        // MEMORY LEAK FIX: Use DisposableEffect to manage WebView lifecycle 
        // OR better, wrap AndroidView in a component that handles destruction?
        // Compose AndroidView does not auto-destroy unless we tell it to?
        // Actually, AndroidView does not unbind.
        
        // We wrap in a block to ensure we can stop loading.
        DisposableEffect(Unit) {
            onDispose { 
                 // We can't easily access the WebView instance here unless we hoist state.
                 // Ideally, we accept that standard WebViews leak a bit, BUT:
                 // A better regex is main priority.
            }
        }
        
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.size(0.dp), // Invisible
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    // Use clean Mobile UA for consistency
                    settings.userAgentString = com.Sourish25.MineBucks.util.Constants.UA_MOBILE
                    
                    val cookieManager = android.webkit.CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    // Inject saved cookies
                    val url = "https://authors.curseforge.com/#/transactions"
                    val cookies = uiState.curseForgeCookies
                    cookieManager.setCookie("https://authors.curseforge.com", cookies)
                    cookieManager.setCookie("https://curseforge.com", cookies)
                    
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                            if (url?.contains("login") == true || url?.contains("signin") == true) {
                                // Redirected to login -> Session Expired
                                viewModel.resetCurseForgeSession()
                            } else if (url?.contains("authors.curseforge.com") == true) {
                                val js = """
                                    (function() {
                                        var body = document.body.innerText;
                                        // ROBUST REGEX: Handle start of line or space/paren OR '>'
                                        // Matches: "1,234.56 Points", "> 123 Points", "(500) Points"
                                        var match = body.match(/(?:^|[\(\s>])([\d,.]+)\s*points/i);
                                        if (match) { return match[1].replace(/,/g, ''); }
                                        return null;
                                    })();
                                """.trimIndent()
                                view?.postDelayed({
                                    view.evaluateJavascript(js) { result ->
                                        if (result != null && result != "null" && result != "\"null\"") {
                                            val points = result.replace("\"", "").toDoubleOrNull()
                                            if (points != null) {
                                                viewModel.saveCurseForgePoints(points)
                                            }
                                        }
                                    }
                                }, 3000)
                            }
                        }
                    }
                    loadUrl(url)
                }
            },
            onRelease = { webView ->
                // MEMORY LEAK FIX: Explicit destruction
                webView.stopLoading()
                webView.destroy()
            }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    
    // Error Handling
    if (uiState.error != null) {
        val errorMessage = uiState.error
        LaunchedEffect(errorMessage) {
            if (errorMessage != null) {
                snackbarHostState.showSnackbar(errorMessage)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pullRefresh(pullRefreshState)
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            
            // 1. Greeting & Settings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         GreetingSection(greeting, uiState.userName, uiState.userAvatarUrl)
                         // Offline Indicator
                         if (uiState.connectionError) {
                             Spacer(modifier = Modifier.width(8.dp))
                             Icon(
                                 imageVector = Icons.Outlined.CloudOff,
                                 contentDescription = "Offline",
                                 tint = MaterialTheme.colorScheme.error,
                                 modifier = Modifier.size(20.dp)
                             )
                         }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(com.Sourish25.MineBucks.R.string.settings))
                    }
                }
            }
            
            // ... (Permission Rationale Placeholder) ...

            // Session Expired Warning
            if (uiState.onboarded && uiState.curseForgeCookies.isNullOrBlank() && uiState.curseForgePoints > 0.0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(com.Sourish25.MineBucks.R.string.session_expired_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            // 2. Total Revenue Card
            item {
                TotalRevenueCard(uiState.totalRevenue, uiState.modrinthRevenueLast24h, uiState.targetCurrency)
            }
            
            item {
                Text(stringResource(com.Sourish25.MineBucks.R.string.breakdown), style = MaterialTheme.typography.titleLarge)
            }
            
            // 3. Platform Breakdown using Expressive shapes
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PlatformCard(
                        title = stringResource(com.Sourish25.MineBucks.R.string.modrinth),
                        amount = uiState.modrinthRevenue,
                        currency = uiState.targetCurrency,
                        color = com.Sourish25.MineBucks.ui.theme.ModrinthGreen,
                        iconRes = com.Sourish25.MineBucks.R.drawable.ic_modrinth,
                        modifier = Modifier.weight(1f),
                        onClick = { uriHandler.openUri("https://modrinth.com/dashboard/revenue") }
                    )
                    PlatformCard(
                        title = stringResource(com.Sourish25.MineBucks.R.string.curseforge),
                        amount = uiState.curseForgeRevenueUSD,
                        currency = uiState.targetCurrency,
                        color = com.Sourish25.MineBucks.ui.theme.CurseForgeOrange,
                        iconRes = com.Sourish25.MineBucks.R.drawable.ic_curseforge,
                        modifier = Modifier.weight(1f),
                        onClick = { uriHandler.openUri("https://authors.curseforge.com/#/transactions") }
                    )
                }
            }
            
            // History Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(com.Sourish25.MineBucks.R.string.revenue_history),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    RevenueHistoryGraph(
                        dailyHistory = uiState.dailyHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                    )
                    
                    // Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.previousWeek() }) {
                            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Week")
                        }
                        
                        val weeksText = if (uiState.currentWeekOffset == 0) 
                             stringResource(com.Sourish25.MineBucks.R.string.current_week) 
                        else 
                             stringResource(com.Sourish25.MineBucks.R.string.weeks_ago, kotlin.math.abs(uiState.currentWeekOffset))
                             
                        Text(
                            text = weeksText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        IconButton(
                            onClick = { viewModel.nextWeek() },
                            enabled = uiState.currentWeekOffset < 0
                        ) {
                            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Week")
                        }
                    }
                }
            }
            
            item {
                Text(
                    text = stringResource(com.Sourish25.MineBucks.R.string.recent_activity),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            // REPLACED RevenueHistoryList with direct Items for Lazy performance
            if (uiState.dailyRecents.isEmpty()) {
                item {
                    Text(stringResource(com.Sourish25.MineBucks.R.string.no_recent_activity), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(uiState.dailyRecents.size) { index ->
                    val daily = uiState.dailyRecents[index]
                    HistoryItem(daily, uiState.targetCurrency)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            }
            
            // Support Us Pill Button (Ad)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                val thanksText = stringResource(com.Sourish25.MineBucks.R.string.support_us_thanks)
                SupportUsButton(
                    onClick = {
                        val activity = context as? android.app.Activity
                        if (activity != null) {
                            com.Sourish25.MineBucks.ads.AdManager.showAd(activity) {
                                android.widget.Toast.makeText(context, thanksText, android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
            
            // Developer Links
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { uriHandler.openUri("https://github.com/Sourish25") }) {
                        Text(stringResource(com.Sourish25.MineBucks.R.string.github), color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(onClick = { uriHandler.openUri("https://ko-fi.com/sourish25") }) {
                        Text(stringResource(com.Sourish25.MineBucks.R.string.kofi), color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Spacer(modifier = Modifier.height(48.dp)) // Final padding
            }
        }

        PullRefreshIndicator(
            refreshing = uiState.isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
        )
    }
}
}

// ... (GreetingSection adapted below) ...

@Composable
fun GreetingSection(greetingRes: Int, name: String, avatarUrl: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (avatarUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        
        Column {
            Text(
                text = "${stringResource(greetingRes)},",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TotalRevenueCard(amount: Double?, last24h: Double?, currencyCode: String) {
    // PERFORMANCE FIX: Only animate when Resumed
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(false) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            isResumed = (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Only run infinite transition if resumed
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    
    val phase by if (isResumed) {
         infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp), // Taller for boldness
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer // Bolder container
        ),
        shape = RoundedCornerShape(32.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Wavy Graph
                WavyGraphCanvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 80.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f), // Subtle wave on bold bg
                    phase = phase
                )
            
            // Content
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = if (amount != null) formatCurrency(amount, currencyCode) else "N/A",
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black), // HUGE text
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(com.Sourish25.MineBucks.R.string.total_revenue),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            
            // Bottom Right Stats (Last 24h)
            if (last24h != null && last24h > 0) {
                 Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 24.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "+${formatCurrency(last24h, currencyCode)}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                     Text(
                        text = stringResource(com.Sourish25.MineBucks.R.string.last_24h),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformCard(
    title: String,
    amount: Double?,
    currency: String,
    color: Color,
    iconRes: Int, // NEW
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(160.dp), // Taller for visuals
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Watermark Icon (Bottom Right, Faded)
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                contentDescription = null,
                tint = color.copy(alpha = 0.1f),
                modifier = Modifier
                    .size(120.dp) // HUGE
                    .align(Alignment.BottomEnd)
                    .offset(x = 30.dp, y = 30.dp) // Clip it slightly
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = iconRes),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp) // Small Header Icon
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Text(
                    text = if (amount != null) formatCurrency(amount, currency) else "N/A",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), // Bolder
                    color = color
                )
            }
        }
    }
}

@Composable
fun WavyGraphCanvas(modifier: Modifier, color: Color, phase: Float) {
    // Optimization: Reuse Path objects to avoid allocation on every frame
    val wavePath = remember { Path() }
    val fillPath = remember { Path() }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Reset before drawing
        wavePath.reset()
        fillPath.reset()
        
        // 1. Calculate the Wave Path (Open)
        
        // Points for the wave
        val points = listOf(0.2f, 0.45f, 0.15f, 0.6f, 0.3f, 0.8f, 0.4f)
        val stepX = width / (points.size - 1)
        
        var previousX = 0f
        var previousY = height // Start low
        
        points.forEachIndexed { index, point ->
            // Animate height slightly based on phase
            val animatedPoint = point + (kotlin.math.sin(phase * 2 * Math.PI + index).toFloat() * 0.08f)
            
            val x = index * stepX
            // Invert Y: 0 is top, height is bottom.
            val y = height - (animatedPoint * height * 0.7f) // Scale height so it doesn't hit top
            
            if (index == 0) {
                wavePath.moveTo(x, y)
                previousX = x
                previousY = y
            } else {
                val controlX1 = previousX + (x - previousX) / 2
                val controlY1 = previousY
                val controlX2 = previousX + (x - previousX) / 2
                val controlY2 = y
                wavePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                previousX = x
                previousY = y
            }
        }
        
        // 2. Draw Fill (Closed Path)
        fillPath.addPath(wavePath)
        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color, color.copy(alpha = 0.05f)), // Fade to near transparent
                startY = 0f,
                endY = height
            )
        )
        
        // 3. Draw Stroke (Open Path ONLY)
        // This avoids drawing the bottom line!
        drawPath(
            path = wavePath,
            color = color.copy(alpha = 1f),
            style = Stroke(
                width = 5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

fun formatCurrency(amount: Double, currency: String): String {
    return try {
        val format = NumberFormat.getCurrencyInstance()
        format.currency = java.util.Currency.getInstance(currency)
        format.format(amount)
    } catch (e: Exception) {
        "$amount $currency"
    }
}
