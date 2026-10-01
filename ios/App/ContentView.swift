import SwiftUI

struct ContentView: View {
    var body: some View {
        NavigationStack {
            List {
                Section("Расчёты") {
                    NavigationLink("Расчёт прибыли") { ProfitCalculatorView() }
                    NavigationLink("Структура команды") { PlaceholderView(title: "Структура команды") }
                    NavigationLink("Дивиденды") { PlaceholderView(title: "Дивиденды") }
                }
            }
            .navigationTitle("С8 расчеты")
        }
    }
}

private struct PlaceholderView: View {
    let title: String
    var body: some View {
        ContentUnavailableView(title, systemImage: "hammer", description: Text("Модуль переносится из Android-версии"))
            .navigationTitle(title)
    }
}
