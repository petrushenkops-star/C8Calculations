package com.pavel.c8calculations.ui.dividend

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividendScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Дивиденды") }, navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).navigationBarsPadding().padding(24.dp)) { Text("Расчет дивидендов будет добавлен на следующих этапах") }
    }
}
