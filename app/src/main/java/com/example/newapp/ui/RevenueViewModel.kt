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

data class RevenueUiState(
    val modrinthRevenue: Double = 0.0,
    val modrinthRevenueLast24h: Double = 0.0, // NEW
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
    val history: List<com.example.newapp.data.database.RevenueSnapshot> = emptyList() // NEW
)

class RevenueViewModel(
    private val repository: RevenueRepository,
    private val dataStoreManager: DataStoreManager,
    private val revenueDao: RevenueDao,
    private val context: android.content.Context // NEW
) : ViewModel() {

    // 1. Internal state for API results (Ephemeral)
    private val _apiState = MutableStateFlow(RevenueUiState())

    // Helper data class for DataStore values
    private data class UserPrefs(
        val token: String?,
        val cfPoints: Double,
        val cfCookies: String?,
        val currency: String,
        val name: String?,
        val theme: String,
        val avatar: String? // NEW
    )

    // 2. Combined Source of Truth
    // Helper class for Settings Flow
    private data class SettingsData(
        val currency: String,
        val name: String?,
        val theme: String,
        val avatar: String?
    )

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

    // 3. History Flow (Database)
    // We want the last 30 days of data for the graph
    // 3. History Flow (Database)
    // We want snapshots for the CURRENT user.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val _historyFlow = dataStoreManager.userId
        .flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else revenueDao.getRecentSnapshots(userId, limit = 30)
        }

    // 4. Exchange Rate Flow
    // We fetch the rate whenever the target currency changes (Settings).
    // This allows us to convert history items synchronously in the combine block.
    private val _rateFlow = _settingsFlow
        .map { settings ->
            if (settings.currency == "USD") 1.0
            else repository.getExchangeRate(settings.currency) // We need to expose this in Repo
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0)

    val uiState: StateFlow<RevenueUiState> = combine(
        _settingsFlow,
        _sessionFlow,
        _apiState,
        _historyFlow,
        _rateFlow
    ) { settings, session, apiState, history, rate ->
        val (currency, name, theme, avatar) = settings
        val (token, cfPoints, cfCookies) = session
        
        // Convert History
        val convertedHistory = history.map { snapshot ->
            // If snapshot is already in target currency (unlikely if we just store USD), skip.
            // But we assume DB stores USD/Base. If DB stores mixed, we normalize to USD first?
            // "RevenueSnapshot" has 'currency' field.
            // Assumption: DB snapshots are in the currency they were fetched in (likely USD for Modrinth/CF).
            // Let's assume input is USD for simplicity as per current Repo logic.
            
            // If we implement mixed currency storage, we'd need more logic. 
            // For now, Repo.convertCurrency assumes input is USD.
            
            val modConverted = snapshot.modrinthRevenue * rate
            val cfConverted = snapshot.curseForgeRevenue * rate
            
            snapshot.copy(
                modrinthRevenue = modConverted,
                curseForgeRevenue = cfConverted,
                currency = currency
            )
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
            history = convertedHistory
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RevenueUiState())

    fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _apiState.value = _apiState.value.copy(isLoading = true)
            
            try {
                // Fetch Modrinth
                val modResult = try {
                    repository.getModrinthRevenue() // Now returns RevenueResult
                } catch (e: Exception) {
                    e.printStackTrace()
                    RevenueRepository.RevenueResult(0.0, 0.0)
                }
                
                val cfRevenue = try {
                    repository.getCurseForgeRevenueInUSD()
                } catch (e: Exception) {
                    e.printStackTrace()
                    0.0
                }
                
                // Convert
                val totalUSD = modResult.totalAmount + cfRevenue
                
                val totalConverted = repository.convertCurrency(totalUSD)
                val modConverted = repository.convertCurrency(modResult.totalAmount)
                val mod24hConverted = repository.convertCurrency(modResult.last24Hours)
                val cfConverted = repository.convertCurrency(cfRevenue)
                
    // 4. Update Widget
                com.example.newapp.widget.RevenueWidget.updateRevenueWidget(context, totalConverted, uiState.value.targetCurrency)

                _apiState.value = _apiState.value.copy(
                    modrinthRevenue = modConverted,
                    modrinthRevenueLast24h = mod24hConverted,
                    curseForgeRevenueUSD = cfConverted,
                    totalRevenue = totalConverted,
                    isLoading = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _apiState.value = _apiState.value.copy(isLoading = false)
            }
        }
    }
    
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
            // No direct refresh needed? Or maybe test it.
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
                val repository = RevenueRepository(
                    com.example.newapp.data.network.ModrinthClient.service, 
                    application.dataStoreManager,
                    application.database.revenueDao()
                )
                
                return RevenueViewModel(repository, application.dataStoreManager, application.database.revenueDao(), application.applicationContext) as T
            }
        }
    }
}
