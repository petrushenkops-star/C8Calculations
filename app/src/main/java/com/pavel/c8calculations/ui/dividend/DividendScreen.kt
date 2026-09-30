package com.pavel.c8calculations.ui.dividend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pavel.c8calculations.calculation.DividendCalculator
import com.pavel.c8calculations.calculation.DividendInput
import com.pavel.c8calculations.calculation.DividendResult
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividendScreen(onBack: () -> Unit) {
    var days by remember { mutableStateOf("10") }
    var c1 by remember { mutableStateOf("0") }
    var c2 by remember { mutableStateOf("0") }
    var c3 by remember { mutableStateOf("0") }
    var c4 by remember { mutableStateOf("0") }
    var c5 by remember { mutableStateOf("0") }
    var c6 by remember { mutableStateOf("0") }
    var result by remember { mutableStateOf<DividendResult?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun calculate() {
        try {
            result = DividendCalculator.calculate(
                DividendInput(
                    days = days.toInt(),
                    c1 = c1.toInt(),
                    c2 = c2.toInt(),
                    c3 = c3.toInt(),
                    c4 = c4.toInt(),
                    c5 = c5.toInt(),
                    c6 = c6.toInt(),
                )
            )
            errorText = null
        } catch (_: NumberFormatException) {
            result = null
            errorText = "Проверьте количество дней и участников"
        } catch (e: IllegalArgumentException) {
            result = null
            errorText = e.message ?: "Проверьте введённые данные"
        }
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
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Ручной расчёт", style = MaterialTheme.typography.titleLarge)
            IntegerField("Количество дней", days) { days = it }
            HorizontalDivider()
            Text("Количество участников", style = MaterialTheme.typography.titleMedium)
            IntegerField("C1", c1) { c1 = it }
            IntegerField("C2", c2) { c2 = it }
            IntegerField("C3", c3) { c3 = it }
            IntegerField("C4", c4) { c4 = it }
            IntegerField("C5", c5) { c5 = it }
            IntegerField("C6", c6) { c6 = it }

            Text("C1 сохраняется для структуры команды, но в формулу дивидендов не входит.", style = MaterialTheme.typography.bodySmall)

            Button(onClick = ::calculate, modifier = Modifier.fillMaxWidth()) {
                Text("Рассчитать")
            }

            errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            result?.let { DividendResultCard(it) }
        }
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
private fun DividendResultCard(result: DividendResult) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Расшифровка", style = MaterialTheme.typography.titleMedium)
            result.levels.forEach { item ->
                Text(
                    "${item.level}: ${format(item.baseAmount)} × 0,02 × ${result.days} × ${item.participants} = ${format(item.amount)} USDT"
                )
            }
            HorizontalDivider()
            Text("Итого: ${format(result.total)} USDT", style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun format(value: BigDecimal): String =
    value.stripTrailingZeros().toPlainString().replace('.', ',')
