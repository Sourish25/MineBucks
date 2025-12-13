package com.example.newapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: RevenueViewModel,
    uiState: RevenueUiState,
    onBack: () -> Unit,
    onConnectCurseForge: () -> Unit
) {
    var modrinthToken by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    val currencies = listOf(
        CurrencyOption("USD", "United States Dollar", "$"),
        CurrencyOption("EUR", "Euro", "€"),
        CurrencyOption("GBP", "British Pound", "£"),
        CurrencyOption("INR", "Indian Rupee", "₹"),
        CurrencyOption("CAD", "Canadian Dollar", "$"),
        CurrencyOption("AUD", "Australian Dollar", "$"),
        CurrencyOption("JPY", "Japanese Yen", "¥"),
        CurrencyOption("CNY", "Chinese Yuan", "¥"),
        CurrencyOption("RUB", "Russian Ruble", "₽"),
        CurrencyOption("BRL", "Brazilian Real", "R$")
    )
    
    val currentCurrency = currencies.find { it.code == uiState.targetCurrency } ?: currencies[0]
    var selectedCurrency by remember { mutableStateOf(currentCurrency) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                   colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val mainScrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(mainScrollState) // Enable scrolling
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 0. Appearance (New)
            Text("Appearance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            ThemeSelectionBar(
                currentTheme = uiState.appTheme,
                onThemeSelected = { viewModel.setTheme(it) }
            )
            
            HorizontalDivider()

            // 1. General Settings
            Text("General", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "${selectedCurrency.symbol}  ${selectedCurrency.code} - ${selectedCurrency.name}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Preferred Currency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    currencies.forEach { currency ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(text = "${currency.code} (${currency.symbol})", fontWeight = FontWeight.Bold)
                                    Text(text = currency.name, style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            onClick = {
                                selectedCurrency = currency
                                viewModel.setCurrency(currency.code)
                                expanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            // 2. Modrinth Settings
            Text("Modrinth", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            OutlinedTextField(
                value = uiState.maskedToken,
                onValueChange = { }, 
                readOnly = true,
                label = { Text("Current Token") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Lock, null) }
            )
             
            OutlinedTextField(
                value = modrinthToken,
                onValueChange = { modrinthToken = it },
                label = { Text("Update Access Token") },
                placeholder = { Text("Enter new token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Button(
                onClick = { 
                    if (modrinthToken.isNotBlank()) {
                        viewModel.saveModrinthToken(modrinthToken) 
                        modrinthToken = "" // Clear after save
                    }
                },
                enabled = modrinthToken.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Token")
            }
            
             TextButton(
                onClick = { uriHandler.openUri("https://modrinth.com/settings/pats") },
                modifier = Modifier.fillMaxWidth()
            ) {
                 Text("Generate New Token")
            }

            HorizontalDivider()

            // 3. CurseForge Settings
            Text("CurseForge", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            if (uiState.curseForgePoints > 0) {
                 Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Connected", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Points: ${uiState.curseForgePoints}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                 Button(
                    onClick = onConnectCurseForge,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Reconnect / Refresh Session")
                }
            } else {
                Button(
                    onClick = onConnectCurseForge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect CurseForge Account")
                }
            }
            
            TextButton(
                onClick = { 
                    viewModel.resetCurseForgeSession() 
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Session (Log Out)", color = MaterialTheme.colorScheme.error)
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Debug Info
            var showDebug by remember { mutableStateOf(false) }
            var debugRawData by remember { mutableStateOf("Click 'Fetch' to see raw API data") }
            val scrollState = rememberScrollState()
            
            if (showDebug) {
                AlertDialog(
                    onDismissRequest = { showDebug = false },
                    title = { Text("Debug Info") },
                    text = {
                        Column {
                            Text("Token: ${uiState.maskedToken}")
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = debugRawData,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .height(200.dp)
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = { 
                                    debugRawData = "Loading..."
                                    viewModel.getDebugInfo { debugRawData = it }
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Text("Fetch Raw API Data")
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showDebug = false }) { Text("Close") } }
                )
            }
            TextButton(
                onClick = { showDebug = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("View Debug Info", style = MaterialTheme.typography.labelSmall)
            }
            
            HorizontalDivider()
            
            // 4. About Developer
            Text("About Developer", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // GitHub Button
                Button(
                    onClick = { uriHandler.openUri("https://github.com/Sourish25") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50), // Pill Shape
                    colors = ButtonDefaults.buttonColors(
                        // Subtle Tonal Button
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(painterResource(id = com.example.newapp.R.drawable.ic_github), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GitHub")
                }
                
                // Ko-fi Button
                Button(
                    onClick = { uriHandler.openUri("https://ko-fi.com/sourish25") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50), // Pill Shape
                    colors = ButtonDefaults.buttonColors(
                        // Subtle Tonal Button (User requested "more subtle", not blue)
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    // Fix: Elevate icon slightly (-2.dp) to align optically with text
                    Icon(
                        painterResource(id = com.example.newapp.R.drawable.ic_ko_fi), 
                        contentDescription = null, 
                        modifier = Modifier.size(20.dp).offset(y = (-2).dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ko-fi")
                }
            }

            // Footer
            Text(
                "Mod Revenue Tracker v1.2", 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun ThemeSelectionBar(
    currentTheme: String,
    onThemeSelected: (String) -> Unit
) {
    val options = listOf("SYSTEM", "LIGHT", "DARK")
    val selectedIndex = when(currentTheme) {
        "LIGHT" -> 1
        "DARK" -> 2
        else -> 0
    }
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        val width = maxWidth
        val tabWidth = width / 3
        
        // Animated Pill
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = tween(400),
            label = "indicator"
        )
        
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        
        // Text Labels
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, option ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // Ripple handled by container or suppressed
                        ) { onThemeSelected(option) },
                    contentAlignment = Alignment.Center
                ) {
                    val isSelected = index == selectedIndex
                    val label = when(option) {
                        "SYSTEM" -> "System"
                        "LIGHT" -> "Light"
                        else -> "Dark"
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if(isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
