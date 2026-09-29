package com.pavel.c8calculations.ui.team

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamRecognitionScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Структура команды") }, navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).navigationBarsPadding().padding(24.dp)) { Text("Распознавание структуры команды будет добавлено на следующих этапах") }
    }
}
