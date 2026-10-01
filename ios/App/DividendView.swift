import SwiftUI
import C8CalculationsCore

struct DividendView: View {
    @ObservedObject var store: TeamStore
    @State private var days = 10
    @State private var result: DividendResult?

    var body: some View {
        Form {
            Section("Период") {
                Stepper("Количество дней: \(days)", value: $days, in: 1...3650)
            }
            Section("Структура команды") {
                ForEach(ParticipantLevel.allCases, id: \.self) { level in
                    LabeledContent(String(describing: level), value: "\(store.count(level))")
                }
            }
            Section {
                Button("Рассчитать дивиденды") {
                    result = DividendCalculator.calculate(.init(days: days, c1: store.count(.C1), c2: store.count(.C2), c3: store.count(.C3), c4: store.count(.C4), c5: store.count(.C5), c6: store.count(.C6)))
                }.frame(maxWidth: .infinity)
            }
            if let result {
                Section("Результат") {
                    ForEach(Array(result.levels.enumerated()), id: \.offset) { _, row in
                        LabeledContent("\(row.level) × \(row.participants)", value: money(row.amount))
                    }
                    LabeledContent("Итого", value: money(result.total)).fontWeight(.semibold)
                }
            }
        }
        .navigationTitle("Дивиденды")
    }

    private func money(_ value: Decimal) -> String {
        let f = NumberFormatter()
        f.numberStyle = .decimal; f.locale = Locale(identifier: "ru_RU"); f.maximumFractionDigits = 2
        return (f.string(from: value as NSDecimalNumber) ?? "\(value)") + " USDT"
    }
}
