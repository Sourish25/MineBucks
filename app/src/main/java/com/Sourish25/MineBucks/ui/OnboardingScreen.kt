package com.Sourish25.MineBucks.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.Sourish25.MineBucks.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: RevenueViewModel,
    onConnectCurseForge: () -> Unit,
    onFinish: () -> Unit
) {
    var modrinthToken by remember { mutableStateOf("") }
    
    // Currency Dropdown State
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
    var expanded by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf(currencies[0]) }
    
    var showModrinthHelp by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    // Language Dropdown
    var languageExpanded by remember { mutableStateOf(false) }
    val currentLangCode = LanguageHelper.getCurrentLanguage()
    val currentLangName = LanguageHelper.supportedLanguages[currentLangCode] ?: "English"

    if (showModrinthHelp) {
        AlertDialog(
            onDismissRequest = { showModrinthHelp = false },
            title = { Text(stringResource(R.string.onboarding_help_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.onboarding_help_desc), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(stringResource(R.string.onboarding_help_p1), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.onboarding_help_p2), style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.onboarding_help_scopes), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.onboarding_help_scopes_val), style = MaterialTheme.typography.bodyMedium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(stringResource(R.string.onboarding_help_warn), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text(stringResource(R.string.onboarding_help_warn_desc), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.onboarding_help_warn_local), style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://modrinth.com/settings/pats") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.onboarding_help_get_token))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModrinthHelp = false }) {
                    Text(stringResource(R.string.onboarding_help_understood))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- Language Selector (Top Right) ---
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            TextButton(onClick = { languageExpanded = true }) {
                Icon(Icons.Default.Language, contentDescription = "Language")
                Spacer(modifier = Modifier.width(4.dp))
                Text(currentLangName)
            }
            DropdownMenu(
                expanded = languageExpanded,
                onDismissRequest = { languageExpanded = false }
            ) {
                LanguageHelper.supportedLanguages.forEach { (code, name) ->
                    DropdownMenuItem(
                        text = { Text(name, fontWeight = if (code == currentLangCode) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            LanguageHelper.setLanguage(code)
                            languageExpanded = false
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Label with Help Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = modrinthToken,
                    onValueChange = { modrinthToken = it },
                    label = { Text(stringResource(R.string.onboarding_modrinth_label)) }, // Removed "(Optional)"
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = { showModrinthHelp = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Help"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // CurseForge Login Button
            Button(
                onClick = { onConnectCurseForge() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_cf_btn))
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Currency Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "${selectedCurrency.symbol}  ${selectedCurrency.code} - ${selectedCurrency.name}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_pref_currency)) },
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
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (modrinthToken.isNotBlank()) viewModel.saveModrinthToken(modrinthToken)
                    viewModel.setCurrency(selectedCurrency.code)
                    onFinish()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.onboarding_start_btn))
            }
        }
    }
}

data class CurrencyOption(val code: String, val name: String, val symbol: String)
