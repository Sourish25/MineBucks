package com.example.newapp.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import com.example.newapp.R
import java.util.UUID

// --- Data Models ---
data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    var webView: WebView,
    var title: MutableState<String> = mutableStateOf("Loading..."),
    var url: MutableState<String> = mutableStateOf(""),
    var favicon: MutableState<Bitmap?> = mutableStateOf(null),
    var isDesktopMode: MutableState<Boolean> = mutableStateOf(true) // Default to desktop
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurseForgeLoginScreen(
    onLoginSuccess: (Double, String) -> Unit, // Added Cookies param
    onClose: () -> Unit
) {
    // --- State ---
    val tabs = remember { mutableStateListOf<BrowserTab>() }
    var activeTabId by remember { mutableStateOf<String?>(null) }
    var showTabSwitcher by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    // Debug Settings
    var globalZoomLevel by remember { mutableStateOf(60) } // Initial scale for desktop fix
    var forceCssZoom by remember { mutableStateOf(false) } // Toggle for JS injection

    // --- Helper Functions ---
    fun createWebView(context: android.content.Context, isDesktop: Boolean = true): WebView {
        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true) // Crucial for Popups
                
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                // Constants for consistent User-Agents
                val UA_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                val UA_MOBILE = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                
                userAgentString = if (isDesktop) UA_DESKTOP else UA_MOBILE
            }
            setInitialScale(globalZoomLevel)

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
        }
    }

    fun addNewTab(context: android.content.Context, url: String? = null, passedWebView: WebView? = null, initialDesktopMode: Boolean = true) {
        val webView = passedWebView ?: createWebView(context, isDesktop = initialDesktopMode)
        val newTab = BrowserTab(webView = webView, isDesktopMode = mutableStateOf(initialDesktopMode))
        
        // Setup Clients
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // SMART UA: Force Mobile for Google Login pages
                if (url?.contains("accounts.google.com") == true && newTab.isDesktopMode.value) {
                    newTab.isDesktopMode.value = false
                    view?.settings?.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                newTab.title.value = view?.title ?: "No Title"
                newTab.url.value = url ?: ""
                
                // CSS Injection Logic
                if (forceCssZoom) {
                    view?.evaluateJavascript("document.body.style.zoom = '0.5';", null)
                }

                // Scraping Logic
                if (url?.contains("authors.curseforge.com") == true) {
                    val js = """
                        (function() {
                            var body = document.body.innerText;
                            // Regex to match "185 points" or "(185 points)", case insensitive
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
                                    // 1. Save Cookies (for background scraping)
                                    val cookies = CookieManager.getInstance().getCookie(url) ?: ""
                                    onLoginSuccess(points, cookies)
                                    
                                    // 2. Auto-Close (Take user to Dashboard)
                                    onClose()
                                }
                            }
                        }
                    }, 2000)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                newTab.title.value = title ?: "Loading..."
            }
            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                newTab.favicon.value = icon
            }

            // --- Multi-Window / Popup Support ---
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                
                // FORCE MOBILE MODE FOR POPUPS (Fixes Google Security Block)
                val popupWebView = createWebView(context, isDesktop = false)
                
                // Recursively add new tab
                addNewTab(context, passedWebView = popupWebView, initialDesktopMode = false)
                
                transport.webView = popupWebView
                resultMsg.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView?) {
                // Find tab with this webview and remove it
                val tabToRemove = tabs.find { it.webView == window }
                if (tabToRemove != null) {
                    tabs.remove(tabToRemove)
                    tabToRemove.webView.destroy()
                    if (activeTabId == tabToRemove.id) {
                        activeTabId = tabs.lastOrNull()?.id
                    }
                }
            }
        }

        if (url != null) webView.loadUrl(url)
        
        tabs.add(newTab)
        activeTabId = newTab.id
    }

    // --- Cleanup on Dispose ---
    DisposableEffect(Unit) {
        onDispose {
            tabs.forEach { tab ->
                tab.webView.stopLoading()
                tab.webView.destroy()
            }
            tabs.clear()
        }
    }

    // --- Main UI Composables ---
    val activeTab = tabs.find { it.id == activeTabId }
    
    // Hardware Back Handler
    BackHandler {
        if (showTabSwitcher) {
            showTabSwitcher = false
        } else if (activeTab?.webView?.canGoBack() == true) {
            activeTab.webView.goBack()
        } else if (tabs.size > 1) {
            // Close current tab if back history empty
            val tabToRemove = activeTab
            if (tabToRemove != null) {
                tabs.remove(tabToRemove)
                tabToRemove.webView.destroy()
                activeTabId = tabs.lastOrNull()?.id
            }
        } else {
            onClose() // Close activity if only one tab left
        }
    }

    Scaffold(
        topBar = {
            Column {
                // Top Bar (Address & Tools)
                Surface(
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Close App Button
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, "Exit")
                        }

                        // Address Bar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Security Icon
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Secure",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                val urlText = activeTab?.url?.value ?: ""
                                val domain = try { java.net.URI(urlText).host ?: urlText } catch(e: Exception) { urlText }
                                
                                Text(
                                    text = if (activeTab?.title?.value?.isNotEmpty() == true) activeTab!!.title.value else domain,
                                    style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Tab Switcher Button (Chrome-style Square with Number)
                        IconButton(onClick = { showTabSwitcher = !showTabSwitcher }) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckBoxOutlineBlank, // Square outline
                                    contentDescription = "Tabs",
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = tabs.size.toString(),
                                    style = TextStyle(fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                )
                            }
                        }

                        // Menu Button
                        Box {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.MoreVert, "Menu")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (activeTab?.isDesktopMode?.value == true) "Switch to Mobile Site" else "Switch to Desktop Site") },
                                    onClick = {
                                        activeTab?.let { tab ->
                                            tab.isDesktopMode.value = !tab.isDesktopMode.value
                                            val ua = if (tab.isDesktopMode.value) 
                                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36" 
                                            else 
                                                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                                            
                                            tab.webView.settings.userAgentString = ua
                                            tab.webView.reload()
                                        }
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (forceCssZoom) "Disable Force Zoom" else "Enable Force Zoom (0.5x)") },
                                    onClick = {
                                        forceCssZoom = !forceCssZoom
                                        activeTab?.webView?.reload()
                                        showMenu = false
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Refresh") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                    onClick = {
                                        activeTab?.webView?.reload()
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Loading Bar
                if (activeTab?.title?.value == "Loading...") {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // --- Tab Content ---
            if (activeTab != null) {
                AndroidView(
                    factory = { activeTab.webView },
                    modifier = Modifier.fillMaxSize(),
                    update = { 
                        // Ensure layout params are match parent
                        if (it.layoutParams.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                            it.layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    }
                )
            } else {
                // No tabs (Shouldn't happen really unless closed all)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No Tabs Open")
                    Button(onClick = { /* Will be handled by launched effect */ }) {
                        Text("Open CurseForge")
                    }
                }
            }
            
            // "Check Rewards" Floating Action Button (Restored)
            if (!showTabSwitcher && activeTab != null) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        activeTab.webView.loadUrl("https://authors.curseforge.com/#/transactions")
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.MonetizationOn, null)
                    Spacer(Modifier.width(8.dp))
                    Text("I'm Logged In -> Check Rewards")
                }
            }

            // --- Tab Switcher Overlay ---
            AnimatedVisibility(
                visible = showTabSwitcher,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 100.dp) // Leave top text visible
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Open Tabs", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(tabs) { tab ->
                        Card(
                            onClick = { 
                                activeTabId = tab.id
                                showTabSwitcher = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (tab.id == activeTabId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tab.favicon.value?.let { 
                                    Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tab.title.value, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                                    Text(tab.url.value, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = {
                                    tabs.remove(tab)
                                    tab.webView.destroy()
                                    if (activeTabId == tab.id) {
                                        activeTabId = tabs.lastOrNull()?.id
                                    }
                                }) {
                                    Icon(Icons.Default.Close, "Close Tab")
                                }
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = { 
                                // Clean logic to add new tab needs Context, so better to just use existing tab functions
                                showTabSwitcher = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Browser")
                        }
                    }
                }
            }
        }
    }

    // Initialize First Tab 
    LaunchedEffect(Unit) {
        if (tabs.isEmpty()) {
            // Need context here
        }
    }
    
    // Hack to get context for initial tab
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        if (tabs.isEmpty()) {
            addNewTab(context, "https://curseforge.com/login")
        }
    }
}
