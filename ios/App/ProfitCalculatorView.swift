import SwiftUI
import C8CalculationsCore

struct ProfitCalculatorView: View {
    enum Mode: String, CaseIterable, Identifiable {
        case date = "По дате"
        case balance = "До баланса"
        case net = "До чистой прибыли"
        var id: Self { self }
    }

    @State private var mode: Mode = .date
    @State private var level: ParticipantLevel = .C5
    @State private var balance = "6000"
    @State private var target = "3000"
    @State private var leaderSignals = 1
    @State private var vip = true
    @State private var l1AtLeast10 = false

    var body: some View {
        Form {
            Picker("Режим", selection: $mode) {
                ForEach(Mode.allCases) { Text($0.rawValue).tag($0) }
            }
            .pickerStyle(.segmented)

            Section("Исходные данные") {
                Picker("Уровень", selection: $level) {
                    ForEach(ParticipantLevel.allCases, id: \.self) { Text(String(describing: $0)).tag($0) }
                }
                TextField("Баланс, USDT", text: $balance).keyboardType(.decimalPad)
                Stepper("Лидерских сигналов: \(leaderSignals)", value: $leaderSignals, in: 0...20)
                Toggle("VIP", isOn: $vip)
                Toggle("L1 ≥ 10", isOn: $l1AtLeast10)
            }

            if mode != .date {
                Section(mode == .balance ? "Целевой баланс" : "Желаемая чистая прибыль") {
                    TextField("USDT", text: $target).keyboardType(.decimalPad)
                }
            }

            Section {
                Button("Рассчитать") { }
                    .frame(maxWidth: .infinity)
            }
        }
        .navigationTitle("Расчёт прибыли")
    }
}
