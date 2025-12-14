package com.example.newapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.newapp.data.repository.DataStoreManager
import com.example.newapp.data.repository.RevenueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.example.newapp.ModRevenueApplication
import com.example.newapp.data.database.RevenueDao
import androidx.lifecycle.viewmodel.CreationExtras

    // Helper for Daily Graph
    data class DailyRevenue(
        val dateMillis: Long,
        val amount: Double
    )

    data class RevenueUiState(
        val modrinthRevenue: Double = 0.0,
        val modrinthRevenueLast24h: Double = 0.0,
        val curseForgePoints: Double = 0.0,
        val curseForgeRevenueUSD: Double = 0.0,
        val totalRevenue: Double = 0.0,
        val targetCurrency: String = "USD",
        val userName: String = "Author",
        val isLoading: Boolean = false,
        val onboarded: Boolean = false,
        val maskedToken: String = "",
        val curseForgeCookies: String = "",
        val appTheme: String = "SYSTEM",
        val userAvatarUrl: String? = null,
        val history: List<com.example.newapp.data.database.RevenueSnapshot> = emptyList(),
        // NEW: Daily Aggregation
        val dailyHistory: List<DailyRevenue> = emptyList(),
        val dailyRecents: List<DailyRevenue> = emptyList(), // New Field
        val currentWeekOffset: Int = 0, // 0 = Current Week, -1 = Previous, etc.
        val isMock: Boolean = false, // Debug Flag
        val error: String? = null, // One-time error message (e.g. "Network Unavailable")
        val connectionError: Boolean = false // Persistent connection status
    )

    class RevenueViewModel(
        private val repository: RevenueRepository,
        private val dataStoreManager: DataStoreManager,
        private val revenueDao: RevenueDao,
        private val context: android.content.Context
    ) : ViewModel() {

    private val _apiState = MutableStateFlow(RevenueUiState())
    private val _weekOffset = MutableStateFlow(0) // Internal state for navigation

    // ... (UserPrefs and SettingsData helpers) 
    private data class SettingsData(
        val currency: String,
        val name: String?,
        val theme: String,
        val avatar: String?
    )
    
    // ... (Combiners)
    
    private val _settingsFlow = combine(
        dataStoreManager.targetCurrency,
        dataStoreManager.userName,
        dataStoreManager.themeMode,
        dataStoreManager.userAvatar
    ) { currency, name, theme, avatar ->
        SettingsData(currency, name, theme, avatar)
    }

    private val _sessionFlow = combine(
        dataStoreManager.modrinthToken,
        dataStoreManager.curseForgePoints,
        dataStoreManager.curseForgeCookies
    ) { token, points, cookies ->
        Triple(token, points, cookies)
    }

    // History Flow: We fetch ALL recent history to calculate daily diffs accurately
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val _historyFlow = dataStoreManager.userId
        .flatMapLatest { userId ->
            // Fetch enough history to cover several weeks if needed, or just all.
            // For safety, let's fetch last 90 days (approx 3 months).
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else revenueDao.getRecentSnapshots(userId, limit = 90)
        }

    private val _rateFlow = _settingsFlow
        .map { settings ->
            if (settings.currency == "USD") 1.0
            else repository.getExchangeRate(settings.currency)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0)

    // Intermediate combiner to avoid > 5 args
    private val _baseDataFlow = combine(
        _settingsFlow,
        _sessionFlow,
        _rateFlow
    ) { settings, session, rate ->
        Triple(settings, session, rate)
    }

    val uiState: StateFlow<RevenueUiState> = combine(
        _baseDataFlow,
        _apiState,
        _historyFlow,
        _weekOffset
    ) { baseData, apiState, history, weekOffset ->
        val (settings, session, rate) = baseData
        val (currency, name, theme, avatar) = settings
        val (token, cfPoints, cfCookies) = session
        
        // 1. Convert History to Target Currency
        val convertedHistory = history.map { snapshot ->
            // Assume snapshot stored in USD-ish base (simple assumption for now)
            val total = (snapshot.modrinthRevenue + snapshot.curseForgeRevenue) * rate
            snapshot.copy(
                modrinthRevenue = snapshot.modrinthRevenue * rate, // localized just for show if needed
                curseForgeRevenue = snapshot.curseForgeRevenue * rate,
                // We fake the 'Modrinth' field to hold the TOTAL for simple processing if we want,
                // but better to keep them separate.
                currency = currency
            )
        }

        // 2. Calculate Daily Earnings (Diffs)
        // Group by Day

        val calendar = java.util.Calendar.getInstance()
        
        // Sort oldest to newest
        val sorted = convertedHistory.sortedBy { it.timestamp }
        
        // Logic: Earning for Day X = (Max Total on Day X) - (Max Total on Day X-1)
        // If Day X-1 is missing, we check previous available snapshot? 
        // User said: "First point as baseline... ignore total... plot next daily increment"
        // Correct approach:
        // Iterate snapshots. 
        // Track 'previousTotal'. 
        // If Date changes, record the delta?
        // Wait, snapshots happen multiple times a day. We want "End of Day Total" - "Start of Day Total"?
        // Simpler: "Earning(Day) = Max(Day) - Max(Day-1)"
        
        // Helper to zero-out time for grouping (UTC)
        fun getDayStart(time: Long): Long {
            val utcCalendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            utcCalendar.timeInMillis = time
            utcCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            utcCalendar.set(java.util.Calendar.MINUTE, 0)
            utcCalendar.set(java.util.Calendar.SECOND, 0)
            utcCalendar.set(java.util.Calendar.MILLISECOND, 0)
            return utcCalendar.timeInMillis
        }
        
        val dailyMaxTotals = sorted
            .groupBy { getDayStart(it.timestamp) }
            .mapValues { (_, snapshots) -> 
                snapshots.maxOf { it.modrinthRevenue + it.curseForgeRevenue } 
            }
            .toSortedMap()

        val dailyEarnings = mutableListOf<DailyRevenue>()
        
        // Calculate deltas
        // We need at least 2 days to show a diff? 
        // User: "initial point as total revenue... ignore... plot next daily increment"
        // Means Day 1 (First ever install) = 0 earning shown? Or assume previous was 0?
        // User said: "if we take initial point ... graph will look inconsistent ... ignore total revenue"
        // So Day 1 = 0. Day 2 = Total(Day2) - Total(Day1).
        
        var previousDayTotal: Double? = null
        
        dailyMaxTotals.forEach { (dayMillis, maxTotal) ->
            if (previousDayTotal != null) {
                val diff = maxTotal - previousDayTotal!!
                // Only add positive days? Or negative too (refunds)?
                // User said "earning per day".
                dailyEarnings.add(DailyRevenue(dayMillis, if (diff > 0) diff else 0.0))
            } else {
                // First day ever.
                // User wants to ignore the massive initial total.
                // So we add 0.0 for the first day recorded.
                dailyEarnings.add(DailyRevenue(dayMillis, 0.0))
            }
            previousDayTotal = maxTotal
        }
        
        // 3. Filter for Selected Week
        // Get "Start of Current Week" (e.g. Sunday)
        val today = getDayStart(System.currentTimeMillis())
        calendar.timeInMillis = today
        // Set to Sunday of this week
        calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)

        
        // Apply Offset
        calendar.add(java.util.Calendar.WEEK_OF_YEAR, weekOffset)
        val targetWeekStart = calendar.timeInMillis
        // val targetWeekEnd = targetWeekStart + (7 * 24 * 60 * 60 * 1000) // Unused
        
        // Filter dailyEarnings that fall within [targetWeekStart, targetWeekEnd)
        // AND Fill in missing days with 0.0
        val weeklyData = mutableListOf<DailyRevenue>()
        for (i in 0 until 7) {
            val dayTime = targetWeekStart + (i * 24 * 60 * 60 * 1000)
            val found = dailyEarnings.find { it.dateMillis == dayTime }
            weeklyData.add(found ?: DailyRevenue(dayTime, 0.0))
        }

        RevenueUiState(
            // DataStore Sources
            curseForgePoints = cfPoints,
            curseForgeCookies = cfCookies ?: "",
            targetCurrency = currency,
            userName = name ?: "Author",
            appTheme = theme,
            userAvatarUrl = avatar, 
            onboarded = !token.isNullOrBlank() || cfPoints > 0.0,
            maskedToken = if (!token.isNullOrBlank()) token.take(8) + "..." else "None",
            
            // API Sources
            modrinthRevenue = apiState.modrinthRevenue,
            modrinthRevenueLast24h = apiState.modrinthRevenueLast24h,
            curseForgeRevenueUSD = apiState.curseForgeRevenueUSD,
            totalRevenue = apiState.totalRevenue,
            isLoading = apiState.isLoading,
            
            // History
            history = convertedHistory,
            dailyHistory = weeklyData, // Expose the 7-day list
            dailyRecents = dailyEarnings.sortedByDescending { it.dateMillis }.take(7), // New Field
            currentWeekOffset = weekOffset,
            isMock = com.example.newapp.BuildConfig.FLAVOR == "mock"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RevenueUiState())

    fun getGreeting(): Int {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> com.example.newapp.R.string.greeting_morning
            in 12..16 -> com.example.newapp.R.string.greeting_afternoon
            else -> com.example.newapp.R.string.greeting_evening
        }
    }
    
    fun nextWeek() {
        if (_weekOffset.value < 0) {
            _weekOffset.value += 1
        }
    }
    
    fun previousWeek() {
        _weekOffset.value -= 1
    }

    fun refreshData() {
        viewModelScope.launch {
            _apiState.value = _apiState.value.copy(isLoading = true)
            
            try {
                // Fetch Modrinth
                val modResult = try {
                    repository.getModrinthRevenue() 
                } catch (e: Exception) {
                    e.printStackTrace()
                    null // Return null on failure
                }
                
                val cfRevenue = try {
                    repository.getCurseForgeRevenueInUSD()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null // Return null on failure
                }
                
                // If Modrinth Failed (Network), mark connection error even if CF (Local) is fine
                var isConnectionError = false
                if (modResult == null) {
                    isConnectionError = true
                }

                // If BOTH failed, stop here and keep old data.
                if (modResult == null && cfRevenue == null) {
                     _apiState.value = _apiState.value.copy(
                         isLoading = false,
                         error = "Failed to refresh. Check your internet connection.",
                         connectionError = true // Mark failure
                     )
                     return@launch
                }
                
                // Use valid data or defaults
                val validModResult = modResult ?: RevenueRepository.RevenueResult(0.0, 0.0) 
                val validCfRevenue = cfRevenue ?: 0.0

                // Convert
                val totalUSD = validModResult.totalAmount + validCfRevenue
                
                val totalConverted = repository.convertCurrency(totalUSD)
                val modConverted = repository.convertCurrency(validModResult.totalAmount)
                val mod24hConverted = repository.convertCurrency(validModResult.last24Hours)
                val cfConverted = repository.convertCurrency(validCfRevenue)
                
    // 4. Update Widget
                com.example.newapp.widget.RevenueWidget.updateRevenueWidget(context, totalConverted, uiState.value.targetCurrency)

                // 5. SAVE SNAPSHOT (Optimized)
                val currentUserId = dataStoreManager.userId.first() ?: "unknown"
                
                if (currentUserId != "unknown") {
                    // Check last snapshot to avoid spam
                    val lastSnapshot = revenueDao.getLatestSnapshot(currentUserId)
                    val currentTotal = validModResult.totalAmount + validCfRevenue
                    val lastTotal = (lastSnapshot?.modrinthRevenue ?: 0.0) + (lastSnapshot?.curseForgeRevenue ?: 0.0)
                    val timeDiff = System.currentTimeMillis() - (lastSnapshot?.timestamp ?: 0L)
                    
                    // Save if: No previous data OR Value Changed OR > 1 Hour passed
                    if (lastSnapshot == null || kotlin.math.abs(currentTotal - lastTotal) > 0.001 || timeDiff > 3600000) {
                         val snapshot = com.example.newapp.data.database.RevenueSnapshot(
                            timestamp = System.currentTimeMillis(),
                            modrinthRevenue = validModResult.totalAmount, // Store RAW (USD)
                            curseForgeRevenue = validCfRevenue,           // Store RAW (USD)
                            currency = "USD", // We store in USD to allow dynamic conversion later
                            userId = currentUserId
                        )
                        revenueDao.insertSnapshot(snapshot)
                    }
                }
                
                // On Success final block, clear error
                _apiState.value = _apiState.value.copy(
                    modrinthRevenue = modConverted,
                    modrinthRevenueLast24h = mod24hConverted,
                    curseForgeRevenueUSD = cfConverted,
                    totalRevenue = totalConverted,
                    isLoading = false,
                    error = null, // Clear error
                    connectionError = isConnectionError // Set based on Modrinth Result
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _apiState.value = _apiState.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.localizedMessage}",
                    connectionError = true // Mark as persistent error
                )
            }
        }
    }
    
    fun clearError() {
        _apiState.value = _apiState.value.copy(error = null)
    }
    
    // ... (rest of methods)
    fun getDebugInfo(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val info = repository.getRawModrinthResponse()
            onResult(info)
        }
    }

    fun saveModrinthToken(token: String) {
        viewModelScope.launch {
            dataStoreManager.saveModrinthToken(token)
            refreshData()
        }
    }
    
    fun saveCurseForgePoints(points: Double) {
        viewModelScope.launch {
            dataStoreManager.saveCurseForgePoints(points)
            refreshData()
        }
    }
    
    fun saveCurseForgeCookies(cookies: String) {
        viewModelScope.launch {
            dataStoreManager.saveCurseForgeCookies(cookies)
        }
    }
    
    fun setCurrency(currency: String) {
        viewModelScope.launch {
            dataStoreManager.saveTargetCurrency(currency)
            refreshData()
        }
    }
    
    fun setTheme(mode: String) {
        viewModelScope.launch {
            dataStoreManager.saveThemeMode(mode)
        }
    }
    
    fun resetCurseForgeSession() {
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()
        viewModelScope.launch {
            dataStoreManager.saveCurseForgePoints(0.0) // Reset saved points
            dataStoreManager.saveCurseForgeCookies("")
        }
    }
    
    fun resetAccount() {
        viewModelScope.launch {
            val userId = dataStoreManager.userId.first()
            if (!userId.isNullOrBlank()) {
                revenueDao.deleteHistoryForUser(userId)
            }
            // Also clear tokens
            dataStoreManager.saveModrinthToken("")
            dataStoreManager.saveCurseForgePoints(0.0)
            dataStoreManager.saveCurseForgeCookies("")
            dataStoreManager.saveUserName("")
            dataStoreManager.saveUserAvatar("")
            dataStoreManager.saveUserId("") // Clear ID
            refreshData()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as ModRevenueApplication
                return RevenueViewModel(application.repository, application.dataStoreManager, application.database.revenueDao(), application.applicationContext) as T
            }
        }
    }
}

