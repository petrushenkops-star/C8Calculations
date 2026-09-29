package com.pavel.c8calculations.ui.profit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pavel.c8calculations.calculation.ProfitSimulationEngine
import com.pavel.c8calculations.model.ParticipantLevel
import com.pavel.c8calculations.model.ProfitSimulationInput
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitScreen(onBack: () -> Unit) {
    var level by remember { mutableStateOf(ParticipantLevel.C1) }
    var balance by remember { mutableStateOf("300") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(30).toString()) }
    var x by remember { mutableStateOf("0") }
    var vip by remember { mutableStateOf(false) }
    var l1 by remember { mutableStateOf("0") }
    var autoUpgrade by remember { mutableStateOf(true) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun calculate() {
        try {
            val from = LocalDate.parse(startDate.trim())
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
                    l1Count = l1.toInt(),
                )
            )
            resultText = buildString {
                appendLine("Дней: $days")
                appendLine("Итоговый уровень: ${result.finalLevel}")
                appendLine("Ожидаемый баланс: ${result.expectedBalance.stripTrailingZeros().toPlainString()} USDT")
                appendLine("Депозит уровня: ${result.currentDeposit.stripTrailingZeros().toPlainString()} USDT")
                appendLine("Прибыль до удержания: ${result.grossProfit.stripTrailingZeros().toPlainString()} USDT")
                appendLine("30%: ${result.withholding.stripTrailingZeros().toPlainString()} USDT")
                append("Чистая прибыль 70%: ${result.netProfit.stripTrailingZeros().toPlainString()} USDT")
            }
            errorText = null
        } catch (_: DateTimeParseException) {
            resultText = null
            errorText = "Дата должна быть в формате ГГГГ-ММ-ДД"
        } catch (_: NumberFormatException) {
            resultText = null
            errorText = "Проверьте числовые поля"
        } catch (e: IllegalArgumentException) {
            resultText = null
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
            Text("Расчёт до даты", style = MaterialTheme.typography.titleLarge)

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
            TextField(
                value = endDate, onValueChange = { endDate = it },
                label = { Text("Дата окончания (ГГГГ-ММ-ДД)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            NumberField("X — дополнительные сигналы", x) { x = it }
            NumberField("Количество L1", l1) { l1 = it }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("VIP")
                Switch(checked = vip, onCheckedChange = { vip = it })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Автопереход между уровнями")
                Switch(checked = autoUpgrade, onCheckedChange = { autoUpgrade = it })
            }

            Button(onClick = ::calculate, modifier = Modifier.fillMaxWidth()) { Text("Рассчитать") }

            errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            resultText?.let {
                Card(Modifier.fillMaxWidth()) {
                    Text(it, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }

            HorizontalDivider()
            Text("Расчёт «До баланса» будет подключён отдельным этапом.")
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
