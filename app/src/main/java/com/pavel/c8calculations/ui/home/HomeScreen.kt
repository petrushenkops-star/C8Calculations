package com.pavel.c8calculations.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onProfit: () -> Unit, onTeam: () -> Unit, onDividend: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("С8 Расчеты") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Расчёты", style = MaterialTheme.typography.headlineMedium)
            HomeActionCard("Прогноз прибыли", "По дате, балансу или чистой прибыли", onProfit)
            HomeActionCard("Структура команды", "Распознавание и корректировка состава", onTeam)
            HomeActionCard("Дивиденды", "Расчёт по сохранённой структуре команды", onDividend)
        }
    }
}

@Composable
private fun HomeActionCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}