package com.pavel.c8calculations.ui.profit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Прогноз прибыли") }, navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).navigationBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("До даты") }
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("До баланса") }
            Text("Расчеты будут добавлены на следующих этапах.")
        }
    }
}
