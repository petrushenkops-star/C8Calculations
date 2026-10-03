package com.pavel.c8calculations.ui.team

import android.provider.MediaStore
import android.net.Uri
import com.pavel.c8calculations.recognition.PdfTeamImporter
import com.pavel.c8calculations.recognition.TeamRecognitionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    var pendingPdf by remember { mutableStateOf<Uri?>(null) }
    var pdfPageCount by remember { mutableStateOf(0) }
    var pageInput by remember { mutableStateOf("1") }

    fun applyResult(result: TeamRecognitionResult?, source: String) {
        recognizing = false
        if (result == null) {
            statusText = "$source: не удалось распознать уровни C0–C6. Прежние значения сохранены; их можно исправить вручную."
        } else if (!result.leaderExcluded) {
            leaderExcluded = false
            statusText = "$source: лидер не определён автоматически. Прежние значения сохранены; проверьте структуру вручную."
        } else {
            counts = (1..6).map { result.counts[it].toString() }
            leaderExcluded = true
            statusText = "$source: найдено карточек — ${result.detectedCards}. Учтены только L1, L2 и L3. Проверьте состав команды."
        }
    }

    fun importPdf(uri: Uri, page: Int? = null) {
        recognizing = true
        statusText = "Чтение PDF…"
        scope.launch {
            try {
                val loaded = PdfTeamImporter.read(context, uri, page)
                val bitmap = loaded.bitmap
                if (bitmap == null) {
                    pendingPdf = uri
                    pdfPageCount = loaded.pageCount
                    pageInput = "1"
                    recognizing = false
                    statusText = "Выберите страницу PDF для распознавания"
                } else {
                    selectedBitmap = bitmap
                    val source = "PDF, страница ${loaded.pageNumber} из ${loaded.pageCount}"
                    if (loaded.textResult != null) {
                        applyResult(loaded.textResult, source)
                    } else {
                        statusText = "$source: распознавание изображения…"
                        TeamLevelRecognizer.recognize(bitmap) { result -> applyResult(result, source) }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                recognizing = false
                statusText = when (error) {
                    is SecurityException -> "Не удалось открыть PDF: файл защищён паролем или нет доступа. Выберите незашифрованный PDF."
                    is IllegalArgumentException -> error.message ?: "Некорректный PDF"
                    else -> "Не удалось прочитать PDF. Проверьте, что файл не повреждён и не защищён паролем."
                }
            }
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importPdf(uri)
    }

    pendingPdf?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingPdf = null; statusText = null },
            title = { Text("Выбор страницы PDF") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Страниц: $pdfPageCount. Выберите страницу с нужной командой. Страницы не суммируются.")
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { value -> if (value.length <= 6 && value.all(Char::isDigit)) pageInput = value },
                        label = { Text("Номер страницы") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pageInput.toIntOrNull()?.let { it in 1..pdfPageCount } == true,
                    onClick = { pendingPdf = null; importPdf(uri, pageInput.toInt()) },
                ) { Text("Распознать") }
            },
            dismissButton = { TextButton(onClick = { pendingPdf = null; statusText = null }) { Text("Отмена") } },
        )
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { MediaStore.Images.Media.getBitmap(context.contentResolver, uri) }
                .onSuccess { bitmap ->
                    selectedBitmap = bitmap
                    recognizing = true
                    statusText = "Распознавание..."
                    leaderExcluded = null
                    TeamLevelRecognizer.recognize(bitmap) { result ->
                        applyResult(result, "Изображение")
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

    val totalParticipants = counts.drop(1).sumOf { it.toIntOrNull()?.coerceAtLeast(0) ?: 0 }

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
            Text("Состав команды", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Загрузите изображение или PDF со структурой команды. В состав входят только участники из L1, L2 и L3. Лидер, C0 и C1 не учитываются.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !recognizing
            ) {
                Text(if (selectedBitmap == null) "Загрузить изображение" else "Выбрать другое изображение")
            }

            OutlinedButton(
                onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !recognizing,
            ) { Text("Загрузить PDF") }
            if (recognizing) LinearProgressIndicator(Modifier.fillMaxWidth())

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
                Text("Результат распознавания", style = MaterialTheme.typography.titleMedium)
                Text("При необходимости количество участников можно исправить вручную.")

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..3).forEach { level ->
                        TeamCountField(
                            label = "C$level",
                            value = counts[level - 1],
                            onValueChange = { newValue ->
                                counts = counts.toMutableList().also { it[level - 1] = newValue }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (4..6).forEach { level ->
                        TeamCountField(
                            label = "C$level",
                            value = counts[level - 1],
                            onValueChange = { newValue ->
                                counts = counts.toMutableList().also { it[level - 1] = newValue }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("$totalParticipants участников", style = MaterialTheme.typography.titleMedium)
                        leaderExcluded?.let { excluded ->
                            Text(
                                if (excluded) "Лидер обнаружен и исключён из подсчёта"
                                else "Лидер не определён автоматически — проверьте состав"
                            )
                        }
                        Text("Учитываются только L1–L3; лидер, C0 и C1 в состав команды не входят")

                    }
                }

            }
        }
    }
}

@Composable
private fun TeamCountField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = value,
        onValueChange = { text ->
            if (text.isEmpty() || text.all(Char::isDigit)) onValueChange(text)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}
