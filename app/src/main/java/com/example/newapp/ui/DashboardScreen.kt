package com.example.newapp.ui

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Added import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import com.example.newapp.ui.theme.ExpressiveMotion
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale


// ... (Existing content)

@Composable
fun DashboardScreen(
    viewModel: RevenueViewModel,
    uiState: RevenueUiState,
    onOpenSettings: () -> Unit
) {
    val scrollState = rememberScrollState()
    val greeting = remember { viewModel.getGreeting() }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current // NEW
    
    // Refresh data on entry
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }
    
    // --- BACKGROUND SCRAPER ---
    // Invisible WebView to keep data fresh using saved cookies
    val context = androidx.compose.ui.platform.LocalContext.current
    if (uiState.onboarded && !uiState.curseForgeCookies.isNullOrBlank()) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.size(0.dp), // Invisible
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    // Use clean Mobile UA for consistency
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                    
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
                                        var match = body.match(/[\(\s](\d[\d,.]*)\s*points/i);
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
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Fix: Explicit background to prevent transparency issues
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ... (Existing UI content)
        // 1. Greeting & Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GreetingSection(greeting, uiState.userName, uiState.userAvatarUrl)
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
        
        // Session Expired Warning
        if (uiState.onboarded && uiState.curseForgeCookies.isNullOrBlank() && uiState.curseForgePoints > 0) {
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
                        "CurseForge Session Expired. Please reconnect in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        // 2. Total Revenue Card
        TotalRevenueCard(uiState.totalRevenue, uiState.modrinthRevenueLast24h, uiState.targetCurrency)
        
        Text("Breakdown", style = MaterialTheme.typography.titleLarge)
        
        // 3. Platform Breakdown using Expressive shapes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlatformCard(
                title = "Modrinth",
                amount = uiState.modrinthRevenue,
                currency = uiState.targetCurrency,
                // Modrinth Green #1BD96A
                color = Color(0xFF1BD96A),
                iconRes = com.example.newapp.R.drawable.ic_modrinth, // New Icon
                modifier = Modifier.weight(1f),
                onClick = { uriHandler.openUri("https://modrinth.com/dashboard/revenue") }
            )
            PlatformCard(
                title = "CurseForge",
                amount = uiState.curseForgeRevenueUSD,
                currency = uiState.targetCurrency,
                // CurseForge Orange #F16436
                color = Color(0xFFF16436),
                iconRes = com.example.newapp.R.drawable.ic_curseforge, // New Icon
                modifier = Modifier.weight(1f),
                onClick = { uriHandler.openUri("https://authors.curseforge.com/#/transactions") }
            )
        }
        
        // History Section
        // Always show history section, even if empty (UI handles empty state)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Revenue History",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        RevenueHistoryGraph(
            dailyHistory = uiState.dailyHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            // Colors handled by defaults now (Material Scheme)
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
            
            Text(
                text = if (uiState.currentWeekOffset == 0) "Current Week" else "${kotlin.math.abs(uiState.currentWeekOffset)} Week(s) Ago",
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        RevenueHistoryList(
            history = uiState.dailyRecents, // Show daily increments
            currency = uiState.targetCurrency,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Support Us Pill Button (Ad)
        SupportUsButton(
            onClick = {
                val activity = context as? android.app.Activity
                if (activity != null) {
                    com.example.newapp.ads.AdManager.showAd(activity) {
                        // On Reward
                        android.widget.Toast.makeText(context, "Thanks for supporting us! ❤️", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Developer Links
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = { uriHandler.openUri("https://github.com/Sourish25") }) {
                Text("GitHub", color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(onClick = { uriHandler.openUri("https://ko-fi.com/sourish25") }) {
                Text("Ko-fi", color = MaterialTheme.colorScheme.secondary)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp)) // Final padding
    }
}
// ... (Rest of file)

@Composable
fun GreetingSection(greeting: String, name: String, avatarUrl: String?) {
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
                text = "$greeting,",
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
fun TotalRevenueCard(amount: Double, last24h: Double, currencyCode: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

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
                    text = formatCurrency(amount, currencyCode),
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black), // HUGE text
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Total Revenue",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            
            // Bottom Right Stats (Last 24h)
            if (last24h > 0) {
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
                        text = "Last 24h",
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
    amount: Double,
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
                    text = formatCurrency(amount, currency),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), // Bolder
                    color = color
                )
            }
        }
    }
}

@Composable
fun WavyGraphCanvas(modifier: Modifier, color: Color, phase: Float) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // 1. Calculate the Wave Path (Open)
        val wavePath = Path()
        
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
            // We want points to define "how high up from bottom" essentially? 
            // Let's say point 1.0 = Top(0), Point 0.0 = Bottom(height).
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
        val fillPath = Path()
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
