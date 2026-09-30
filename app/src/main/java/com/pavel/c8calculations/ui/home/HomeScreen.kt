package com.pavel.c8calculations.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onProfit: () -> Unit, onTeam: () -> Unit, onDividend: () -> Unit) {
    val context = LocalContext.current
    val profit = context.getSharedPreferences("profit_screen_settings", android.content.Context.MODE_PRIVATE)
    val team = context.getSharedPreferences("team_structure", android.content.Context.MODE_PRIVATE)

    val level = profit.getString("level", "C1") ?: "C1"
    val balance = profit.getString("balance", "300") ?: "300"
    val mode = when (profit.getString("calculationMode", "DATE")) {
        "BALANCE" -> "До баланса"
        "NET_PROFIT" -> "До прибыли"
        else -> "До даты"
    }
    val c2 = team.getInt("c2", 0)
    val c3 = team.getInt("c3", 0)
    val c4 = team.getInt("c4", 0)
    val c5 = team.getInt("c5", 0)
    val c6 = team.getInt("c6", 0)
    val total = c2 + c3 + c4 + c5 + c6

    Scaffold(topBar = { TopAppBar(title = { Text("С8 Расчеты") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Расчёты", style = MaterialTheme.typography.headlineMedium)
            HomeActionCard("Прогноз прибыли", "$level · Баланс $balance USDT\nПоследний режим: $mode", onProfit)
            HomeActionCard(
                "Структура команды",
                if (total == 0) "Команда ещё не задана" else "$total участников\nC2 $c2; C3 $c3; C4 $c4; C5 $c5; C6 $c6",
                onTeam
            )
            HomeActionCard(
                "Дивиденды",
                if (total == 0) "Сначала задайте структуру команды" else "Расчёт по сохранённой команде · $total участников",
                onDividend
            )
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
