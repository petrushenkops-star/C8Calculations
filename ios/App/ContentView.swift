import SwiftUI

struct ContentView: View {
    @StateObject private var teamStore = TeamStore()

    var body: some View {
        NavigationStack {
            List {
                Section("Расчёты") {
                    NavigationLink("Расчёт прибыли") { ProfitCalculatorView() }
                    NavigationLink("Структура команды") { TeamStructureView(store: teamStore) }
                    NavigationLink("Дивиденды") { DividendView(store: teamStore) }
                }
            }
            .navigationTitle("С8 расчеты")
        }
    }
}
