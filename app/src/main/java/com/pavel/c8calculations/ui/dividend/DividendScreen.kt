package com.pavel.c8calculations.ui.dividend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pavel.c8calculations.calculation.DividendCalculator
import com.pavel.c8calculations.calculation.DividendInput
import com.pavel.c8calculations.calculation.DividendResult
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividendScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("team_structure", android.content.Context.MODE_PRIVATE)
    }
    var days by remember { mutableStateOf("10") }
    val c1 = preferences.getInt("c1", 0)
    val c2 = preferences.getInt("c2", 0)
    val c3 = preferences.getInt("c3", 0)
    val c4 = preferences.getInt("c4", 0)
    val c5 = preferences.getInt("c5", 0)
    val c6 = preferences.getInt("c6", 0)
    val totalParticipants = c1 + c2 + c3 + c4 + c5 + c6
    var result by remember { mutableStateOf<DividendResult?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun calculate() {
        try {
            result = DividendCalculator.calculate(
                DividendInput(days.toInt(), c1, c2, c3, c4, c5, c6)
            )
            errorText = null
        } catch (_: NumberFormatException) {
            result = null
            errorText = "Проверьте количество дней"
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
            Text("Расчёт дивидендов", style = MaterialTheme.typography.titleLarge)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Структура команды", style = MaterialTheme.typography.titleMedium)
                    Text("C1 — $c1   C2 — $c2   C3 — $c3")
                    Text("C4 — $c4   C5 — $c5   C6 — $c6")
                    Text("Всего участников: $totalParticipants")
                }
            }
            Text(
                "Для изменения состава команды используйте раздел «Структура команды».",
                style = MaterialTheme.typography.bodySmall
            )
            Text("C1 учитывается в структуре команды, но не входит в формулу дивидендов.", style = MaterialTheme.typography.bodySmall)

            IntegerField("Количество дней", days) { days = it }

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
