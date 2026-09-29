package com.pavel.c8calculations.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onProfit: () -> Unit, onTeam: () -> Unit, onDividend: () -> Unit, onSettings: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("С8 Расчеты") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onProfit, modifier = Modifier.fillMaxWidth()) { Text("Прогноз прибыли") }
            Button(onClick = onTeam, modifier = Modifier.fillMaxWidth()) { Text("Структура команды") }
            Button(onClick = onDividend, modifier = Modifier.fillMaxWidth()) { Text("Дивиденды") }
            Button(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Настройки") }
        }
    }
}
