package com.pavel.c8calculations.ui.dividend

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pavel.c8calculations.calculation.DividendCalculator
import com.pavel.c8calculations.calculation.DividendInput
import com.pavel.c8calculations.calculation.DividendResult
import com.pavel.c8calculations.calculation.TeamReportData
import com.pavel.c8calculations.calculation.TeamReportFormatter
import com.pavel.c8calculations.recognition.TeamScanStorage
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividendScreen(onBack: () -> Unit, onEditTeam: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("team_structure", android.content.Context.MODE_PRIVATE)
    }
    val reportPreferences = remember {
        context.getSharedPreferences("team_report", android.content.Context.MODE_PRIVATE)
    }

    val dividendPreferences = remember {
        context.getSharedPreferences("dividend_parameters", android.content.Context.MODE_PRIVATE)
    }
    var days by remember { mutableStateOf(dividendPreferences.getString("days", "10") ?: "10") }
    var history by remember { mutableStateOf(DividendHistoryStorage.load(context)) }
    var showHistory by remember { mutableStateOf(false) }
    var showAllHistory by remember { mutableStateOf(false) }
    var confirmHistoryClear by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var viewedHistoryAt by remember { mutableStateOf<Long?>(null) }
    var historyError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(days) {
        dividendPreferences.edit().putString("days", days).apply()
    }
    val c1 = preferences.getInt("c1", 0)
    val c2 = preferences.getInt("c2", 0)
    val c3 = preferences.getInt("c3", 0)
    val c4 = preferences.getInt("c4", 0)
    val c5 = preferences.getInt("c5", 0)
    val c6 = preferences.getInt("c6", 0)
    val totalParticipants = c2 + c3 + c4 + c5 + c6
    val officialParticipants = TeamReportFormatter.officialParticipants(c2, c3, c4, c5, c6)
    val directParticipants = preferences.getInt("l1_count", 0)
    val leaderName = preferences.getString("leader_name", "").orEmpty()
    val leaderUid = preferences.getString("leader_uid", "").orEmpty()

    var result by remember { mutableStateOf<DividendResult?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var meetings by remember { mutableStateOf(reportPreferences.getString("meetings", "0").orEmpty()) }
    var sergeant by remember { mutableStateOf(reportPreferences.getString("sergeant", "0").orEmpty()) }
    var corporal by remember { mutableStateOf(reportPreferences.getString("corporal", "0").orEmpty()) }
    var corporate by remember { mutableStateOf(reportPreferences.getString("corporate", "0").orEmpty()) }
    var teamProblems by remember { mutableStateOf(reportPreferences.getString("team_problems", "").orEmpty()) }
    var tenDayPlan by remember { mutableStateOf(reportPreferences.getString("ten_day_plan", "").orEmpty()) }
    var reportText by remember { mutableStateOf<String?>(null) }
    var reportDividendResult by remember { mutableStateOf<DividendResult?>(null) }
    var reportScanBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(meetings, sergeant, corporal, corporate, teamProblems, tenDayPlan) {
        reportPreferences.edit()
            .putString("meetings", meetings)
            .putString("sergeant", sergeant)
            .putString("corporal", corporal)
            .putString("corporate", corporate)
            .putString("team_problems", teamProblems)
            .putString("ten_day_plan", tenDayPlan)
            .apply()
    }

    fun dividendInput(): DividendInput = DividendInput(days.toInt(), c1, c2, c3, c4, c5, c6)
    fun dividendResult(): DividendResult = DividendCalculator.calculate(dividendInput())

    fun calculate() {
        try {
            result = dividendResult()
            viewedHistoryAt = null
            historyError = null
            // Only an explicit calculation is added to history, not report generation.
            runCatching { DividendHistoryStorage.add(context, dividendInput()) }
                .onSuccess { history = it }
                .onFailure { historyError = "Не удалось сохранить историю расчёта" }
            errorText = null
        } catch (_: NumberFormatException) {
            result = null
            errorText = "Проверьте количество дней"
        } catch (e: IllegalArgumentException) {
            result = null
            errorText = e.message ?: "Проверьте введённые данные"
        }
    }

    fun formReport() {
        val calculatedDividends = try {
            dividendResult()
        } catch (_: NumberFormatException) {
            errorText = "Проверьте количество дней"
            return
        } catch (e: IllegalArgumentException) {
            errorText = e.message ?: "Проверьте введённые данные"
            return
        }

        errorText = null
        result = calculatedDividends
        reportDividendResult = calculatedDividends
        reportScanBitmap = TeamScanStorage.load(context)

        val reportDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        reportText = TeamReportFormatter.format(
            TeamReportData(
                reportDate = reportDate,
                leaderName = leaderName,
                leaderUid = leaderUid,
                meetings = meetings,
                c1 = c1,
                c2 = c2,
                c3 = c3,
                c4 = c4,
                c5 = c5,
                c6 = c6,
                sergeant = sergeant,
                corporal = corporal,
                directParticipants = directParticipants,
                officialParticipants = officialParticipants,
                corporate = corporate,
                teamProblems = teamProblems,
                tenDayPlan = tenDayPlan,
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Дивиденды") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Расчёт дивидендов", style = MaterialTheme.typography.titleLarge)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Команда · $totalParticipants участников",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onEditTeam) { Text("Изменить") }
                    }
                    Text("С2 $c2 · С3 $c3 · С4 $c4 · С5 $c5 · С6 $c6")
                }
            }
            if (totalParticipants == 0) {
                Text(
                    "Структура команды ещё не задана. Сначала загрузите её в разделе «Структура команды».",
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    IntegerField("Количество дней", days) { days = it }
                }
                Button(onClick = ::calculate, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("Рассчитать")
                }
            }

            errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            historyError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            viewedHistoryAt?.let { timestamp ->
                Text(
                    "Просмотр сохранённого расчёта от ${formatHistoryDate(timestamp)}. " +
                        "Текущие данные команды не изменены.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            result?.let { DividendResultCard(it) }

            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("История · ${history.size}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { showHistory = !showHistory }) {
                    Text(if (showHistory) "Скрыть" else "Показать")
                }
            }
            if (showHistory) {
                if (history.isEmpty()) {
                    Text("История пока пуста. Нажмите «Рассчитать», чтобы сохранить первый расчёт.")
                } else {
                    history.take(if (showAllHistory) DividendHistoryCodec.MAX_ENTRIES else 8).forEach { entry ->
                        val savedResult = remember(entry) { DividendCalculator.calculate(entry.input) }
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(formatHistoryDate(entry.createdAtMillis), style = MaterialTheme.typography.labelMedium)
                                    Text("${entry.input.days} дн. · ${savedResult.levels.sumOf { it.participants }} участников")
                                    Text("${format(savedResult.total)} USDT", style = MaterialTheme.typography.titleMedium)
                                }
                                TextButton(onClick = {
                                    result = savedResult
                                    viewedHistoryAt = entry.createdAtMillis
                                    errorText = null
                                }) { Text("Открыть") }
                            }
                        }
                    }
                    if (history.size > 8) {
                        TextButton(onClick = { showAllHistory = !showAllHistory }) {
                            Text(if (showAllHistory) "Показать меньше" else "Показать ещё (${history.size - 8})")
                        }
                    }
                    TextButton(onClick = { confirmHistoryClear = true }) { Text("Очистить историю") }
                }
            }

            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Отчёт", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { showReport = !showReport }) {
                    Text(if (showReport) "Скрыть" else "Развернуть")
                }
            }
            if (showReport) {
                Text(
                "Дата, лидер, UID, C1–C6, количество прямых L1 и количество официальных участников C2–C6 заполняются автоматически.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Лидер: ${leaderName.ifBlank { "—" }}", style = MaterialTheme.typography.titleMedium)
                    Text("UID: ${leaderUid.ifBlank { "—" }}")
                    Text("C1 $c1; C2 $c2; C3 $c3; C4 $c4; C5 $c5; C6 $c6")
                    Text("Прямые (L1): $directParticipants")
                    Text("Всего оф. уч. (C2–C6): $officialParticipants")
                }
            }

            if (leaderName.isBlank() || leaderUid.isBlank()) {
                Text(
                    "Для автоматического заполнения имени и UID лидера заново загрузите PDF в разделе «Структура команды».",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (!TeamScanStorage.exists(context)) {
                Text(
                    "Чтобы добавить скан структуры в отчёт, один раз заново загрузите изображение или PDF в разделе «Структура команды».",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ReportNumberField("1. Совещаний", meetings) { meetings = it }
            ReportNumberField("8. Сержант", sergeant) { sergeant = it }
            ReportNumberField("9. Капрал", corporal) { corporal = it }
            ReportNumberField("12. Корпоратив", corporate) { corporate = it }
            ReportTextField("13. Проблемы команды", teamProblems) { teamProblems = it }
            ReportTextField("14. План на ближайшие 10 дней", tenDayPlan) { tenDayPlan = it }

            Button(
                onClick = ::formReport,
                enabled = leaderName.isNotBlank() && leaderUid.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сформировать отчёт")
            }

            reportText?.let { text ->
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                reportScanBitmap?.let { bitmap ->
                    Text("Скан структуры команды", style = MaterialTheme.typography.titleMedium)
                    Card(Modifier.fillMaxWidth()) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Скан структуры команды в отчёте",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(8.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }

                reportDividendResult?.let { dividends ->
                    DividendResultCard(dividends, compact = true)
                }
            }
            } // showReport
        }
    }

    if (confirmHistoryClear) {
        AlertDialog(
            onDismissRequest = { confirmHistoryClear = false },
            title = { Text("Очистить историю?") },
            text = { Text("Все сохранённые расчёты дивидендов будут удалены. Текущая команда останется без изменений.") },
            confirmButton = {
                TextButton(onClick = {
                    DividendHistoryStorage.clear(context)
                    history = emptyList()
                    showAllHistory = false
                    confirmHistoryClear = false
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmHistoryClear = false }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun IntegerField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ReportNumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = { text -> if (text.isEmpty() || text.all(Char::isDigit)) onValueChange(text) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ReportTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = 2,
        maxLines = 5,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DividendResultCard(result: DividendResult, compact: Boolean = false) {
    var showFormulas by remember(result, compact) { mutableStateOf(compact) }
    val activeLevels = result.levels.filter { it.participants > 0 }
    val participantCount = result.levels.sumOf { it.participants }
    val perDay = result.total.divide(BigDecimal(result.days))

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(if (compact) 12.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Итого за ${result.days} дн.", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${format(result.total)} USDT", style = MaterialTheme.typography.headlineMedium)
            Text("${participantCount} участников · ${format(perDay)} USDT в день",
                style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            if (activeLevels.isEmpty()) {
                Text("Нет участников С2–С6 для начисления дивидендов.")
            }
            activeLevels.forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${item.level} · ${item.participants} чел.", Modifier.weight(1f))
                    Text("${format(item.amount)} USDT")
                }
            }
            TextButton(onClick = { showFormulas = !showFormulas }) {
                Text(if (showFormulas) "Скрыть формулы" else "Показать формулы")
            }
            if (showFormulas) {
                result.levels.forEach { item ->
                    Text(
                        "${item.level}: ${format(item.baseAmount)} × 2% × ${result.days} × ${item.participants} = ${format(item.amount)} USDT",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun formatHistoryDate(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

private fun format(value: BigDecimal): String {
    val plain = value.stripTrailingZeros().toPlainString()
    val parts = plain.split('.', limit = 2)
    val sign = if (parts[0].startsWith("-")) "-" else ""
    val integer = parts[0].removePrefix("-")
    val grouped = integer.reversed().chunked(3).joinToString("\u202F").reversed()
    return sign + grouped + if (parts.size == 2) "," + parts[1] else ""
}
