import SwiftUI
import C8CalculationsCore

struct ProfitCalculatorView: View {
    enum Mode: String, CaseIterable, Identifiable {
        case date = "По дате", balance = "До баланса", net = "До чистой прибыли"
        var id: Self { self }
    }

    private struct ResultViewModel {
        let date: Date
        let days: Int
        let signals: Int
        let balance: Decimal
        let level: ParticipantLevel
        let deposit: Decimal
        let gross: Decimal
        let commission: Decimal
        let net: Decimal
    }

    @State private var mode: Mode = .date
    @State private var level: ParticipantLevel = .C5
    @State private var balanceText = "6000"
    @State private var targetText = "3000"
    @State private var endDate = Date()
    @State private var leaderSignals = 1
    @State private var vip = true
    @State private var l1AtLeast10 = false
    @State private var result: ResultViewModel?
    @State private var errorText: String?

    var body: some View {
        Form {
            Picker("Режим", selection: $mode) {
                ForEach(Mode.allCases) { Text($0.rawValue).tag($0) }
            }.pickerStyle(.segmented)

            Section("Исходные данные") {
                Picker("Уровень", selection: $level) {
                    ForEach(ParticipantLevel.allCases, id: \.self) { Text(String(describing: $0)).tag($0) }
                }
                TextField("Баланс, USDT", text: $balanceText).keyboardType(.decimalPad)
                Stepper("Лидерских сигналов: \(leaderSignals)", value: $leaderSignals, in: 0...20)
                Toggle("VIP", isOn: $vip)
                Toggle("L1 ≥ 10", isOn: $l1AtLeast10)
            }

            if mode == .date {
                Section("Дата расчёта") {
                    DatePicker("До даты", selection: $endDate, displayedComponents: .date)
                }
            } else {
                Section(mode == .balance ? "Целевой баланс" : "Желаемая чистая прибыль") {
                    TextField("USDT", text: $targetText).keyboardType(.decimalPad)
                }
            }

            Section {
                Button("Рассчитать", action: calculate).frame(maxWidth: .infinity)
            }

            if let errorText {
                Section { Text(errorText).foregroundStyle(.red) }
            }
            if let r = result {
                Section("Результат") {
                    LabeledContent("Дата", value: r.date.formatted(date: .numeric, time: .omitted))
                    LabeledContent("Дней", value: "\(r.days)")
                    LabeledContent("Сигналов", value: "\(r.signals)")
                    LabeledContent("Баланс", value: money(r.balance))
                    LabeledContent("Уровень", value: String(describing: r.level))
                    LabeledContent("Депозит", value: money(r.deposit))
                    LabeledContent("Прибыль", value: money(r.gross))
                    LabeledContent("Комиссия 30%", value: money(r.commission))
                    LabeledContent("Чистая прибыль", value: money(r.net))
                }
            }
        }
        .navigationTitle("Расчёт прибыли")
    }

    private func calculate() {
        errorText = nil
        result = nil
        guard let balance = decimal(balanceText), balance >= 0 else {
            errorText = "Введите корректный баланс"; return
        }
        let calendar = Calendar.current
        let start = calendar.startOfDay(for: Date())
        switch mode {
        case .date:
            let end = calendar.startOfDay(for: endDate)
            guard end >= start else { errorText = "Дата не может быть раньше сегодняшней"; return }
            let days = (calendar.dateComponents([.day], from: start, to: end).day ?? 0) + 1
            let r = ProfitSimulationEngine.simulate(startDate: start, numberOfDays: days, startingLevel: level, startingBalance: balance, x: leaderSignals, isVip: vip, l1Count: l1AtLeast10 ? 10 : 0, calendar: calendar)
            result = .init(date: end, days: days, signals: r.days.reduce(0) { $0 + $1.signalCount }, balance: r.expectedBalance, level: r.finalLevel, deposit: r.currentDeposit, gross: r.grossProfit, commission: r.withholding, net: r.netProfit)
        case .balance, .net:
            guard let target = decimal(targetText), target >= 0 else { errorText = "Введите корректную цель"; return }
            if mode == .balance {
                let r = TargetBalanceCalculator.calculate(startDate: start, startingLevel: level, startingBalance: balance, targetBalance: target, x: leaderSignals, isVip: vip, l1Count: l1AtLeast10 ? 10 : 0, calendar: calendar)
                result = map(r)
            } else {
                let r = TargetNetProfitCalculator.calculate(startDate: start, startingLevel: level, startingBalance: balance, targetNetProfit: target, x: leaderSignals, isVip: vip, l1Count: l1AtLeast10 ? 10 : 0, calendar: calendar)
                result = map(r)
            }
        }
    }

    private func map(_ r: TargetCalculationResult) -> ResultViewModel {
        .init(date: r.reachedDate, days: r.daysCount, signals: r.totalSignals, balance: r.reachedBalance, level: r.finalLevel, deposit: r.currentDeposit, gross: r.grossProfit, commission: r.withholding, net: r.netProfit)
    }

    private func decimal(_ text: String) -> Decimal? {
        Decimal(string: text.replacingOccurrences(of: ",", with: "."))
    }

    private func money(_ value: Decimal) -> String {
        let f = NumberFormatter()
        f.numberStyle = .decimal; f.locale = Locale(identifier: "ru_RU")
        f.maximumFractionDigits = 2; f.minimumFractionDigits = 0
        return (f.string(from: value as NSDecimalNumber) ?? "\(value)") + " USDT"
    }
}
