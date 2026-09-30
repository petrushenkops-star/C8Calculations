package com.pavel.c8calculations.ui.team

import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pavel.c8calculations.recognition.TeamLevelRecognizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamRecognitionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("team_structure", android.content.Context.MODE_PRIVATE)
    }
    var selectedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var counts by remember {
        mutableStateOf((1..6).map { preferences.getInt("c$it", 0).toString() })
    }
    var statusText by remember { mutableStateOf<String?>(null) }
    var leaderExcluded by remember {
        mutableStateOf(if (preferences.contains("leaderExcluded")) preferences.getBoolean("leaderExcluded", false) else null)
    }
    var recognizing by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { MediaStore.Images.Media.getBitmap(context.contentResolver, uri) }
                .onSuccess { bitmap ->
                    selectedBitmap = bitmap
                    recognizing = true
                    statusText = "Распознавание..."
                    leaderExcluded = null
                    TeamLevelRecognizer.recognize(bitmap) { result ->
                        recognizing = false
                        if (result == null) {
                            statusText = "Не удалось распознать уровни C1–C6"
                        } else {
                            counts = (1..6).map { result.counts[it].toString() }
                            leaderExcluded = result.leaderExcluded
                            statusText = "Распознавание завершено"
                        }
                    }
                }
                .onFailure {
                    statusText = "Не удалось открыть изображение"
                    leaderExcluded = null
                }
        }
    }

    LaunchedEffect(counts, leaderExcluded) {
        val editor = preferences.edit()
        counts.forEachIndexed { index, value -> editor.putInt("c${index + 1}", value.toIntOrNull()?.coerceAtLeast(0) ?: 0) }
        leaderExcluded?.let { editor.putBoolean("leaderExcluded", it) }
        editor.apply()
    }

    val totalParticipants = counts.sumOf { it.toIntOrNull()?.coerceAtLeast(0) ?: 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Структура команды") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !recognizing
            ) {
                Text(if (selectedBitmap == null) "Загрузить структуру команды" else "Выбрать другое изображение")
            }

            selectedBitmap?.let { bitmap ->
                Card(Modifier.fillMaxWidth()) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Загруженная структура команды",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            statusText?.let { Text(it) }

            if (selectedBitmap != null || counts.any { (it.toIntOrNull() ?: 0) > 0 }) {
                HorizontalDivider()
                Text("Результат распознавания", style = MaterialTheme.typography.titleLarge)
                Text("При необходимости количество участников можно исправить вручную.")

                (1..6).forEach { level ->
                    TeamCountField(
                        label = "C$level",
                        value = counts[level - 1],
                        onValueChange = { newValue ->
                            counts = counts.toMutableList().also { it[level - 1] = newValue }
                        }
                    )
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Всего участников: $totalParticipants", style = MaterialTheme.typography.titleMedium)
                        leaderExcluded?.let { excluded ->
                            Text(
                                if (excluded) "Лидер обнаружен и исключён из подсчёта"
                                else "Лидер на изображении не обнаружен"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamCountField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = { text ->
            if (text.isEmpty() || text.all(Char::isDigit)) onValueChange(text)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
