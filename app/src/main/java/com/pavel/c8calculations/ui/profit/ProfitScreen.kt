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
import androidx.compose.ui.unit.dp
import com.pavel.c8calculations.calculation.ProfitSimulationEngine
import com.pavel.c8calculations.calculation.TargetBalanceCalculator
import com.pavel.c8calculations.model.ParticipantLevel
import com.pavel.c8calculations.model.ProfitSimulationInput
import com.pavel.c8calculations.model.TargetBalanceInput
import java.math.BigDecimal
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
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(30).toString()) }
    var x by remember { mutableStateOf(preferences.getString("leaderSignals", "0") ?: "0") }
    var vip by remember { mutableStateOf(preferences.getBoolean("vip", false)) }
    var l1AtLeast10 by remember { mutableStateOf(preferences.getBoolean("l1AtLeast10", false)) }
    var autoUpgrade by remember { mutableStateOf(preferences.getBoolean("autoUpgrade", true)) }
    var targetBalance by remember { mutableStateOf(preferences.getString("targetBalance", "1000") ?: "1000") }
    var usdtRubRate by remember { mutableStateOf(preferences.getString("usdtRubRate", "80") ?: "80") }
    var targetMode by remember { mutableStateOf(preferences.getBoolean("targetMode", false)) }
    var dateResultText by remember { mutableStateOf<String?>(null) }
    var targetResultText by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(level, balance, x, vip, l1AtLeast10, autoUpgrade, targetBalance, usdtRubRate, targetMode) {
        preferences.edit()
            .putString("level", level.name)
            .putString("balance", balance)
            .putString("leaderSignals", x)
            .putBoolean("vip", vip)
            .putBoolean("l1AtLeast10", l1AtLeast10)
            .putBoolean("autoUpgrade", autoUpgrade)
            .putString("targetBalance", targetBalance)
            .putString("usdtRubRate", usdtRubRate)
            .putBoolean("targetMode", targetMode)
            .apply()
    }

    fun calculate() {
        try {
            val from = LocalDate.parse(startDate.trim())
            val rubRate = BigDecimal(usdtRubRate.trim().replace(',', '.'))
            require(rubRate.signum() > 0) { "Курс USDT должен быть больше 0" }
            if (targetMode) {
                val targetResult = TargetBalanceCalculator.calculate(TargetBalanceInput(from, level, BigDecimal(balance.trim().replace(',', '.')), BigDecimal(targetBalance.trim().replace(',', '.')), autoUpgrade, x.toInt(), vip, if (l1AtLeast10) 10 else 0))
                targetResultText = buildString {
                    appendLine("Дата достижения: ${targetResult.reachedDate}")
                    appendLine("После сигнала: ${targetResult.reachedAfterSignal}")
                    appendLine("Календарных дней: ${targetResult.daysCount}")
                    appendLine("Сигналов: ${targetResult.totalSignals}")
                    appendLine("Доход за период: ${targetResult.totalIncome.stripTrailingZeros().toPlainString()} USDT")
                    appendLine("Достигнутый баланс: ${targetResult.reachedBalance.stripTrailingZeros().toPlainString()} USDT")
                    appendLine("Итоговый уровень: ${targetResult.finalLevel}")
                    appendLine("Депозит уровня: ${targetResult.currentDeposit.stripTrailingZeros().toPlainString()} USDT")
                    appendLine("Прибыль до удержания: ${targetResult.grossProfit.stripTrailingZeros().toPlainString()} USDT")
                    appendLine("Комиссия 30%: ${targetResult.withholding.stripTrailingZeros().toPlainString()} USDT")
                    appendLine("Чистая прибыль 70%: ${targetResult.netProfit.stripTrailingZeros().toPlainString()} USDT")
                    append("Чистая прибыль, ₽: ${targetResult.netProfit.multiply(rubRate).stripTrailingZeros().toPlainString()} ₽")
                }
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
            dateResultText = buildString {
                appendLine("Дней: $days")
                appendLine("Итоговый уровень: ${result.finalLevel}")
                appendLine("Ожидаемый баланс: ${result.expectedBalance.stripTrailingZeros().toPlainString()} USDT")
                appendLine("Депозит уровня: ${result.currentDeposit.stripTrailingZeros().toPlainString()} USDT")
                appendLine("Прибыль до удержания: ${result.grossProfit.stripTrailingZeros().toPlainString()} USDT")
                appendLine("Комиссия 30%: ${result.withholding.stripTrailingZeros().toPlainString()} USDT")
                appendLine("Чистая прибыль 70%: ${result.netProfit.stripTrailingZeros().toPlainString()} USDT")
                append("Чистая прибыль, ₽: ${result.netProfit.multiply(rubRate).stripTrailingZeros().toPlainString()} ₽")
            }
            errorText = null
        } catch (_: DateTimeParseException) {
            if (targetMode) targetResultText = null else dateResultText = null
            errorText = "Дата должна быть в формате ГГГГ-ММ-ДД"
        } catch (_: NumberFormatException) {
            if (targetMode) targetResultText = null else dateResultText = null
            errorText = "Проверьте числовые поля"
        } catch (e: IllegalArgumentException) {
            if (targetMode) targetResultText = null else dateResultText = null
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !targetMode, onClick = { targetMode = false; dateResultText = null; errorText = null }, label = { Text("До даты") })
                FilterChip(selected = targetMode, onClick = { targetMode = true; targetResultText = null; errorText = null }, label = { Text("До баланса") })
            }
            Text(if (targetMode) "Расчёт до баланса" else "Расчёт до даты", style = MaterialTheme.typography.titleLarge)

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
            if (targetMode) {
                NumberField("Целевой баланс, USDT", targetBalance) { targetBalance = it }
            } else {
                TextField(
                    value = endDate, onValueChange = { endDate = it },
                    label = { Text("Дата окончания (ГГГГ-ММ-ДД)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            NumberField("Кол-во лидерских сигналов", x) { x = it }
            NumberField("Курс USDT, ₽", usdtRubRate) { usdtRubRate = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Кол-во участников на L1 ≥ 10")
                Checkbox(checked = l1AtLeast10, onCheckedChange = { l1AtLeast10 = it })
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("VIP")
                Checkbox(checked = vip, onCheckedChange = { vip = it })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Автопереход между уровнями")
                Checkbox(checked = autoUpgrade, onCheckedChange = { autoUpgrade = it })
            }

            Button(onClick = ::calculate, modifier = Modifier.fillMaxWidth()) { Text("Рассчитать") }

            errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            (if (targetMode) targetResultText else dateResultText)?.let {
                Card(Modifier.fillMaxWidth()) {
                    Text(it, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
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
