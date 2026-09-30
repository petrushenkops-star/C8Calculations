package com.pavel.c8calculations.ui.profit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pavel.c8calculations.calculation.ProfitSimulationEngine
import com.pavel.c8calculations.calculation.TargetBalanceCalculator
import com.pavel.c8calculations.calculation.TargetNetProfitCalculator
import com.pavel.c8calculations.model.ParticipantLevel
import com.pavel.c8calculations.model.ProfitSimulationDay
import com.pavel.c8calculations.model.ProfitSimulationInput
import com.pavel.c8calculations.model.TargetBalanceInput
import com.pavel.c8calculations.model.TargetNetProfitInput
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("profit_screen_settings", android.content.Context.MODE_PRIVATE)
    }
    var level by remember {
        mutableStateOf(
            runCatching { ParticipantLevel.valueOf(preferences.getString("level", ParticipantLevel.C1.name)!!) }
                .getOrDefault(ParticipantLevel.C1)
        )
    }
    var balance by remember { mutableStateOf(preferences.getString("balance", "300") ?: "300") }
    var startDate by remember { mutableStateOf(preferences.getString("startDate", LocalDate.now().toString()) ?: LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(preferences.getString("endDate", LocalDate.now().plusDays(30).toString()) ?: LocalDate.now().plusDays(30).toString()) }
    var x by remember { mutableStateOf(preferences.getString("leaderSignals", "0") ?: "0") }
    var vip by remember { mutableStateOf(preferences.getBoolean("vip", false)) }
    var l1AtLeast10 by remember { mutableStateOf(preferences.getBoolean("l1AtLeast10", false)) }
    var autoUpgrade by remember { mutableStateOf(preferences.getBoolean("autoUpgrade", true)) }
    var targetBalance by remember { mutableStateOf(preferences.getString("targetBalance", "1000") ?: "1000") }
    var targetNetProfit by remember { mutableStateOf(preferences.getString("targetNetProfit", "100") ?: "100") }
    var targetNetProfitCurrency by remember { mutableStateOf(preferences.getString("targetNetProfitCurrency", "USDT") ?: "USDT") }
    var usdtRubRate by remember { mutableStateOf(preferences.getString("usdtRubRate", "80") ?: "80") }
    var calculationMode by remember { mutableStateOf(preferences.getString("calculationMode", if (preferences.getBoolean("targetMode", false)) "BALANCE" else "DATE") ?: "DATE") }
    var dateResultText by remember { mutableStateOf<String?>(null) }
    var targetResultText by remember { mutableStateOf<String?>(null) }
    var netProfitResultText by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var detailDays by remember { mutableStateOf<List<ProfitSimulationDay>>(emptyList()) }
    var showDetails by remember { mutableStateOf(false) }

    LaunchedEffect(level, balance, startDate, endDate, x, vip, l1AtLeast10, autoUpgrade, targetBalance, targetNetProfit, targetNetProfitCurrency, usdtRubRate, calculationMode) {
        preferences.edit()
            .putString("level", level.name)
            .putString("balance", balance)
            .putString("startDate", startDate)
            .putString("endDate", endDate)
            .putString("leaderSignals", x)
            .putBoolean("vip", vip)
            .putBoolean("l1AtLeast10", l1AtLeast10)
            .putBoolean("autoUpgrade", autoUpgrade)
            .putString("targetBalance", targetBalance)
            .putString("targetNetProfit", targetNetProfit)
            .putString("targetNetProfitCurrency", targetNetProfitCurrency)
            .putString("usdtRubRate", usdtRubRate)
            .putString("calculationMode", calculationMode)
            .apply()
    }

    fun calculate() {
        showDetails = false
        detailDays = emptyList()
        try {
            val from = LocalDate.parse(startDate.trim())
            val rubRate = BigDecimal(usdtRubRate.trim().replace(',', '.'))
            require(rubRate.signum() > 0) { "Курс USDT должен быть больше 0" }
            if (calculationMode == "BALANCE") {
                val targetResult = TargetBalanceCalculator.calculate(TargetBalanceInput(from, level, BigDecimal(balance.trim().replace(',', '.')), BigDecimal(targetBalance.trim().replace(',', '.')), autoUpgrade, x.toInt(), vip, if (l1AtLeast10) 10 else 0))
                targetResultText = formatTargetResult(targetResult.reachedDate, targetResult.reachedAfterSignal, targetResult.daysCount, targetResult.totalSignals, targetResult.totalIncome, targetResult.reachedBalance, targetResult.finalLevel.name, targetResult.currentDeposit, targetResult.grossProfit, targetResult.withholding, targetResult.netProfit, rubRate)
                detailDays = targetResult.days
                errorText = null
                return
            }
            if (calculationMode == "NET_PROFIT") {
                val enteredTarget = BigDecimal(targetNetProfit.trim().replace(',', '.'))
                require(enteredTarget.signum() >= 0) { "Целевая чистая прибыль не может быть отрицательной" }
                val targetInUsdt = if (targetNetProfitCurrency == "RUB")
                    enteredTarget.divide(rubRate, 8, RoundingMode.HALF_UP)
                else enteredTarget
                val targetResult = TargetNetProfitCalculator.calculate(TargetNetProfitInput(from, level, BigDecimal(balance.trim().replace(',', '.')), targetInUsdt, autoUpgrade, x.toInt(), vip, if (l1AtLeast10) 10 else 0))
                netProfitResultText = formatTargetResult(targetResult.reachedDate, targetResult.reachedAfterSignal, targetResult.daysCount, targetResult.totalSignals, targetResult.totalIncome, targetResult.reachedBalance, targetResult.finalLevel.name, targetResult.currentDeposit, targetResult.grossProfit, targetResult.withholding, targetResult.netProfit, rubRate)
                detailDays = targetResult.days
                errorText = null
                return
            }
            val to = LocalDate.parse(endDate.trim())
            require(!to.isBefore(from)) { "Дата окончания не может быть раньше даты начала" }
            val days = ChronoUnit.DAYS.between(from, to).toInt() + 1
            val result = ProfitSimulationEngine.simulate(
                ProfitSimulationInput(
                    startDate = from,
                    numberOfDays = days,
                    startingLevel = level,
                    startingBalance = BigDecimal(balance.trim().replace(',', '.')),
                    autoUpgradeEnabled = autoUpgrade,
                    x = x.toInt(),
                    isVip = vip,
                    l1Count = if (l1AtLeast10) 10 else 0,
                )
            )
            detailDays = result.days
            dateResultText = buildString {
                appendLine("Дней: $days")
                appendLine("Итоговый уровень: ${result.finalLevel}")
                appendLine("Ожидаемый баланс: ${result.expectedBalance.stripTrailingZeros().toPlainString()} USDT")
                appendLine("Депозит уровня: ${result.currentDeposit.stripTrailingZeros().toPlainString()} USDT")
                appendLine("Прибыль до удержания: ${result.grossProfit.stripTrailingZeros().toPlainString()} USDT")
                appendLine("Комиссия 30%: ${result.withholding.stripTrailingZeros().toPlainString()} USDT")
                appendLine("Чистая прибыль: ${result.netProfit.stripTrailingZeros().toPlainString()} USDT")
                append("Чистая прибыль, ₽: ${result.netProfit.multiply(rubRate).stripTrailingZeros().toPlainString()} ₽")
            }
            errorText = null
        } catch (_: DateTimeParseException) {
            if (calculationMode == "BALANCE") targetResultText = null else if (calculationMode == "NET_PROFIT") netProfitResultText = null else dateResultText = null
            errorText = "Дата должна быть в формате ГГГГ-ММ-ДД"
        } catch (_: NumberFormatException) {
            if (calculationMode == "BALANCE") targetResultText = null else if (calculationMode == "NET_PROFIT") netProfitResultText = null else dateResultText = null
            errorText = "Проверьте числовые поля"
        } catch (e: IllegalArgumentException) {
            if (calculationMode == "BALANCE") targetResultText = null else if (calculationMode == "NET_PROFIT") netProfitResultText = null else dateResultText = null
            errorText = e.message ?: "Проверьте введённые данные"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Прогноз прибыли") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModeChip(calculationMode == "DATE", "До даты", Modifier.weight(1f)) { calculationMode = "DATE"; dateResultText = null; errorText = null }
                ModeChip(calculationMode == "BALANCE", "До баланса", Modifier.weight(1f)) { calculationMode = "BALANCE"; targetResultText = null; errorText = null }
                ModeChip(calculationMode == "NET_PROFIT", "До прибыли", Modifier.weight(1f)) { calculationMode = "NET_PROFIT"; netProfitResultText = null; errorText = null }
            }
            Text(when (calculationMode) { "BALANCE" -> "Расчёт до баланса"; "NET_PROFIT" -> "Расчёт до чистой прибыли"; else -> "Расчёт до даты" }, style = MaterialTheme.typography.headlineSmall)
            Text(
                "Исходные данные",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("Текущий уровень")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ParticipantLevel.entries.forEach { item ->
                    FilterChip(
                        selected = level == item,
                        onClick = {
                            level = item
                            balance = com.pavel.c8calculations.calculation.LevelConfiguration
                                .forLevel(item).deposit.stripTrailingZeros().toPlainString()
                        },
                        label = { Text(item.name) }
                    )
                }
            }

            NumberField("Текущий баланс, USDT", balance) { balance = it }
            TextField(
                value = startDate, onValueChange = { startDate = it },
                label = { Text("Дата начала (ГГГГ-ММ-ДД)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (calculationMode == "BALANCE") {
                NumberField("Целевой баланс, USDT", targetBalance) { targetBalance = it }
            } else if (calculationMode == "NET_PROFIT") {
                Text("Целевая чистая прибыль")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetNetProfitCurrency == "USDT",
                        onClick = { targetNetProfitCurrency = "USDT"; netProfitResultText = null; errorText = null },
                        label = { Text("USDT") }
                    )
                    FilterChip(
                        selected = targetNetProfitCurrency == "RUB",
                        onClick = { targetNetProfitCurrency = "RUB"; netProfitResultText = null; errorText = null },
                        label = { Text("₽") }
                    )
                }
                NumberField(
                    if (targetNetProfitCurrency == "RUB") "Целевая чистая прибыль, ₽" else "Целевая чистая прибыль, USDT",
                    targetNetProfit
                ) { targetNetProfit = it }
            } else {
                TextField(
                    value = endDate, onValueChange = { endDate = it },
                    label = { Text("Дата окончания (ГГГГ-ММ-ДД)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            NumberField("Кол-во лидерских сигналов", x) { x = it }
            NumberField("Курс USDT, ₽", usdtRubRate) { usdtRubRate = it }
            HorizontalDivider()
            Text("Параметры", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("L1 ≥ 10")
                Switch(checked = l1AtLeast10, onCheckedChange = { l1AtLeast10 = it })
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("VIP")
                Switch(checked = vip, onCheckedChange = { vip = it })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Автопереход уровней")
                Switch(checked = autoUpgrade, onCheckedChange = { autoUpgrade = it })
            }

            Button(onClick = ::calculate, modifier = Modifier.fillMaxWidth()) { Text("Рассчитать") }

            errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            (when (calculationMode) { "BALANCE" -> targetResultText; "NET_PROFIT" -> netProfitResultText; else -> dateResultText })?.let {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Результат", style = MaterialTheme.typography.titleMedium)
                        Text(it, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (detailDays.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showDetails) "Скрыть подробный расчёт" else "Подробный расчёт по дням")
                }
                if (showDetails) {
                    DailyDetails(detailDays)
                }
            }
        }
    }
}


@Composable
private fun ModeChip(selected: Boolean, text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = if (selected) {
        ButtonDefaults.filledTonalButtonColors()
    } else {
        ButtonDefaults.outlinedButtonColors()
    }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        colors = colors
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

private fun formatTargetResult(
    reachedDate: LocalDate, reachedAfterSignal: Int, daysCount: Int, totalSignals: Int,
    totalIncome: BigDecimal, reachedBalance: BigDecimal, finalLevel: String,
    currentDeposit: BigDecimal, grossProfit: BigDecimal, withholding: BigDecimal,
    netProfit: BigDecimal, rubRate: BigDecimal
): String = buildString {
    appendLine("Дата достижения: $reachedDate")
    appendLine("После сигнала: $reachedAfterSignal")
    appendLine("Календарных дней: $daysCount")
    appendLine("Сигналов: $totalSignals")
    appendLine("Доход за период: ${totalIncome.stripTrailingZeros().toPlainString()} USDT")
    appendLine("Достигнутый баланс: ${reachedBalance.stripTrailingZeros().toPlainString()} USDT")
    appendLine("Итоговый уровень: $finalLevel")
    appendLine("Депозит уровня: ${currentDeposit.stripTrailingZeros().toPlainString()} USDT")
    appendLine("Прибыль до удержания: ${grossProfit.stripTrailingZeros().toPlainString()} USDT")
    appendLine("Комиссия 30%: ${withholding.stripTrailingZeros().toPlainString()} USDT")
    appendLine("Чистая прибыль: ${netProfit.stripTrailingZeros().toPlainString()} USDT")
    append("Чистая прибыль, ₽: ${netProfit.multiply(rubRate).stripTrailingZeros().toPlainString()} ₽")
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}


@Composable
private fun DailyDetails(days: List<ProfitSimulationDay>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Подробный расчёт по дням", style = MaterialTheme.typography.titleMedium)
        days.forEach { day ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val transition = if (day.levelForNextDay != day.levelUsed) " → ${day.levelForNextDay}" else ""
                    Text("${day.date} • ${day.levelUsed}${transition}", style = MaterialTheme.typography.titleSmall)
                    Text("Сигналов: ${day.signalCount}")
                    Text("Доход за сигнал: ${day.incomePerSignal.stripTrailingZeros().toPlainString()} USDT")
                    Text("Доход за день: ${day.dailyIncome.stripTrailingZeros().toPlainString()} USDT")
                    Text("Баланс: ${day.balanceAfter.stripTrailingZeros().toPlainString()} USDT")
                }
            }
        }
    }
}
