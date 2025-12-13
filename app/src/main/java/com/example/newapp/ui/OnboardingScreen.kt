package com.example.newapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalUriHandler

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

    if (showModrinthHelp) {
        AlertDialog(
            onDismissRequest = { showModrinthHelp = false },
            title = { Text("About Modrinth Tokens") },
            text = {
                Column {
                    Text("We need specific permissions to show your data:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("• Read user data: To display your username.", style = MaterialTheme.typography.bodySmall)
                    Text("• Read payouts: To calculate your revenue.", style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("REQUIRED SCOPES:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Text("analytics, payouts, user", style = MaterialTheme.typography.bodyMedium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("⚠️ SECURITY WARNING:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("This token grants access to your account data. Do NOT share it with anyone else.", style = MaterialTheme.typography.bodySmall)
                    Text("This app saves it locally on your device only.", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://modrinth.com/settings/pats") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Get Token (modrinth.com)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModrinthHelp = false }) {
                    Text("Understood")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome",
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
                label = { Text("Modrinth Access Token") }, // Removed "(Optional)"
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
            Text("Connect CurseForge (Login)")
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
            Text("Start Tracking")
        }
    }
}

data class CurrencyOption(val code: String, val name: String, val symbol: String)
