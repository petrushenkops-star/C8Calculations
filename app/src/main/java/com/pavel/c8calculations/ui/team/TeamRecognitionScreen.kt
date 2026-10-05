package com.pavel.c8calculations.ui.team

import android.content.SharedPreferences
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pavel.c8calculations.recognition.PdfTeamImporter
import com.pavel.c8calculations.recognition.TeamDetectedCard
import com.pavel.c8calculations.recognition.TeamLevelRecognizer
import com.pavel.c8calculations.recognition.TeamRecognitionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

enum class TeamSortField { LINE, LEVEL, NAME, DATE }

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
    var participantListFromPdf by remember {
        mutableStateOf(preferences.getBoolean("participants_from_pdf", false))
    }
    var participants by remember {
        mutableStateOf(if (participantListFromPdf) loadParticipants(preferences) else emptyList())
    }
    var sortField by remember { mutableStateOf(TeamSortField.LEVEL) }
    var sortAscending by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var leaderExcluded by remember {
        mutableStateOf(if (preferences.contains("leaderExcluded")) preferences.getBoolean("leaderExcluded", false) else null)
    }
    var recognizing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var pendingPdf by remember { mutableStateOf<Uri?>(null) }
    var pdfPageCount by remember { mutableStateOf(0) }
    var pageInput by remember { mutableStateOf("1") }

    fun applyResult(result: TeamRecognitionResult?, source: String, includeParticipantList: Boolean) {
        recognizing = false
        if (result == null) {
            statusText = "$source: не удалось распознать уровни C0–C6. Прежние значения сохранены; их можно исправить вручную."
        } else if (!result.leaderExcluded) {
            leaderExcluded = false
            statusText = "$source: лидер не определён автоматически. Прежние значения сохранены; проверьте структуру вручную."
        } else {
            counts = (1..6).map { result.counts[it].toString() }
            leaderExcluded = true
            if (includeParticipantList) {
                val recognizedParticipants = result.cards.filter {
                    !it.excludedAsLeader && (it.depth?.let { depth -> depth in 1..3 } == true) && it.level in 2..6
                }
                val leader = result.cards.firstOrNull { it.excludedAsLeader }
                preferences.edit()
                    .putString("leader_name", leader?.name.orEmpty())
                    .putString("leader_uid", leader?.uid.orEmpty())
                    .putInt("l1_count", recognizedParticipants.count { it.depth == 1 })
                    .apply()
                participants = recognizedParticipants
                participantListFromPdf = true
                statusText = "$source: найдено карточек — ${result.detectedCards}. В список команды включено ${recognizedParticipants.size} участников из L1–L3."
            } else {
                participants = emptyList()
                participantListFromPdf = false
                statusText = "$source: найдено карточек — ${result.detectedCards}. Учтены только L1, L2 и L3."
            }
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
                        applyResult(loaded.textResult, source, includeParticipantList = true)
                    } else {
                        statusText = "$source: распознавание изображения страницы…"
                        TeamLevelRecognizer.recognize(bitmap, extractParticipantDetails = true) { result ->
                            applyResult(result, source, includeParticipantList = true)
                        }
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
            participants = emptyList()
            participantListFromPdf = false
            preferences.edit()
                .remove("leader_name")
                .remove("leader_uid")
                .remove("l1_count")
                .apply()
            runCatching { MediaStore.Images.Media.getBitmap(context.contentResolver, uri) }
                .onSuccess { bitmap ->
                    selectedBitmap = bitmap
                    recognizing = true
                    statusText = "Распознавание..."
                    leaderExcluded = null
                    TeamLevelRecognizer.recognize(bitmap) { result ->
                        applyResult(result, "Изображение", includeParticipantList = false)
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

    LaunchedEffect(participants, participantListFromPdf) {
        val editor = preferences.edit().putBoolean("participants_from_pdf", participantListFromPdf)
        if (participantListFromPdf) {
            editor.putString("participants", encodeParticipants(participants))
        } else {
            editor.remove("participants")
        }
        editor.apply()
    }

    val totalParticipants = counts.drop(1).sumOf { it.toIntOrNull()?.coerceAtLeast(0) ?: 0 }
    val sortedParticipants = remember(participants, sortField, sortAscending) {
        sortParticipants(participants, sortField, sortAscending)
    }

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
                "Загрузите изображение или PDF со структурой команды. В состав входят только L1–L3; лидер, C0 и C1 не учитываются. Для PDF дополнительно распознаются уровень, имя, UID и дата каждой карточки.",
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

            if (participantListFromPdf && participants.isNotEmpty()) {
                HorizontalDivider()
                Text("Список участников команды", style = MaterialTheme.typography.titleLarge)

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SortChip("Линия", TeamSortField.LINE, sortField, sortAscending) { selected ->
                        if (sortField == selected) sortAscending = !sortAscending else { sortField = selected; sortAscending = true }
                    }
                    SortChip("Уровень", TeamSortField.LEVEL, sortField, sortAscending) { selected ->
                        if (sortField == selected) sortAscending = !sortAscending else { sortField = selected; sortAscending = true }
                    }
                    SortChip("Имя", TeamSortField.NAME, sortField, sortAscending) { selected ->
                        if (sortField == selected) sortAscending = !sortAscending else { sortField = selected; sortAscending = true }
                    }
                    SortChip("Дата", TeamSortField.DATE, sortField, sortAscending) { selected ->
                        if (sortField == selected) sortAscending = !sortAscending else { sortField = selected; sortAscending = true }
                    }
                }

                sortedParticipants.forEachIndexed { index, participant ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                "${index + 1}. C${participant.level}  •  L${participant.depth ?: "—"}  •  ${participant.name.ifBlank { "—" }}",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "UID: ${participant.uid.ifBlank { "—" }}  •  Дата: ${participant.date.ifBlank { "—" }}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    field: TeamSortField,
    selectedField: TeamSortField,
    ascending: Boolean,
    onClick: (TeamSortField) -> Unit,
) {
    val selected = field == selectedField
    FilterChip(
        selected = selected,
        onClick = { onClick(field) },
        label = { Text(if (selected) "$label ${if (ascending) "↑" else "↓"}" else label) },
    )
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

private fun sortParticipants(
    participants: List<TeamDetectedCard>,
    field: TeamSortField,
    ascending: Boolean,
): List<TeamDetectedCard> {
    val comparator = when (field) {
        TeamSortField.LINE -> compareBy<TeamDetectedCard> { it.depth ?: Int.MAX_VALUE }
            .thenBy { it.level }
            .thenBy { it.name.lowercase() }
        TeamSortField.LEVEL -> compareBy<TeamDetectedCard> { it.level }.thenBy { it.name.lowercase() }
        TeamSortField.NAME -> compareBy<TeamDetectedCard> { it.name.isBlank() }.thenBy { it.name.lowercase() }
        TeamSortField.DATE -> compareBy<TeamDetectedCard> { dateSortKey(it.date) }.thenBy { it.name.lowercase() }
    }
    return participants.sortedWith(if (ascending) comparator else comparator.reversed())
}

private fun dateSortKey(value: String): Long {
    val parts = value.trim().split(Regex("[./-]"))
    if (parts.size != 3) return Long.MAX_VALUE
    return runCatching {
        val day = parts[0].toInt()
        val month = parts[1].toInt()
        val rawYear = parts[2].toInt()
        val year = if (rawYear < 100) 2000 + rawYear else rawYear
        LocalDate.of(year, month, day).toEpochDay()
    }.getOrDefault(Long.MAX_VALUE)
}

private fun encodeParticipants(participants: List<TeamDetectedCard>): String {
    val array = JSONArray()
    participants.forEach { participant ->
        array.put(JSONObject().apply {
            put("level", participant.level)
            put("depth", participant.depth ?: JSONObject.NULL)
            put("name", participant.name)
            put("uid", participant.uid)
            put("date", participant.date)
        })
    }
    return array.toString()
}

private fun loadParticipants(preferences: SharedPreferences): List<TeamDetectedCard> {
    val raw = preferences.getString("participants", null) ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    TeamDetectedCard(
                        level = item.optInt("level", 0),
                        x = 0,
                        y = 0,
                        excludedAsLeader = false,
                        depth = if (item.isNull("depth")) null else item.optInt("depth"),
                        name = item.optString("name"),
                        uid = item.optString("uid"),
                        date = item.optString("date"),
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}
