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
        let dayDetails: [ProfitSimulationDay]
    }

    @AppStorage("profit.mode") private var modeRaw = Mode.date.rawValue
    @AppStorage("profit.level") private var levelRaw = ParticipantLevel.C5.rawValue
    @AppStorage("profit.balance") private var balanceText = "6000"
    @AppStorage("profit.target") private var targetText = "3000"
    @AppStorage("profit.endDate") private var endDateTimestamp = Date().timeIntervalSince1970
    @AppStorage("profit.leaderSignals") private var leaderSignals = 1
    @AppStorage("profit.vip") private var vip = true
    @AppStorage("profit.l1AtLeast10") private var l1AtLeast10 = false
    @AppStorage("profit.usdtRubRate") private var usdtRubRateText = "88"
    @State private var result: ResultViewModel?
    @State private var errorText: String?

    private var mode: Mode {
        get { Mode(rawValue: modeRaw) ?? .date }
        nonmutating set { modeRaw = newValue.rawValue }
    }
    private var level: ParticipantLevel {
        get { ParticipantLevel(rawValue: levelRaw) ?? .C5 }
        nonmutating set { levelRaw = newValue.rawValue }
    }
    private var endDate: Date {
        get { Date(timeIntervalSince1970: endDateTimestamp) }
        nonmutating set { endDateTimestamp = newValue.timeIntervalSince1970 }
    }

    var body: some View {
        Form {
            Picker("Режим", selection: Binding(get: { mode }, set: { newMode in\n                mode = newMode\n                result = nil\n                errorText = nil\n            })) {
                ForEach(Mode.allCases) { Text($0.rawValue).tag($0) }
            }.pickerStyle(.segmented)

            Section("Исходные данные") {
                Picker("Уровень", selection: Binding(get: { level }, set: { level = $0 })) {
                    ForEach(ParticipantLevel.allCases, id: \.self) { Text(String(describing: $0)).tag($0) }
                }
                TextField("Баланс, USDT", text: $balanceText).keyboardType(.decimalPad)
                Stepper("Лидерских сигналов: \(leaderSignals)", value: $leaderSignals, in: 0...20)
                Toggle("VIP", isOn: $vip)
                Toggle("L1 ≥ 10", isOn: $l1AtLeast10)
                TextField("Курс USDT, ₽", text: $usdtRubRateText).keyboardType(.decimalPad)
            }

            if mode == .date {
                Section("Дата расчёта") {
                    DatePicker("До даты", selection: Binding(get: { endDate }, set: { endDate = $0 }), displayedComponents: .date)
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
                    if let rate = decimal(usdtRubRateText), rate > 0 {
                        LabeledContent("Чистая прибыль, ₽", value: rubles(r.net * rate))
                    }
                }
                if !r.dayDetails.isEmpty {
                    Section("Отчёт по дням") {
                        ForEach(Array(r.dayDetails.enumerated()), id: \.offset) { _, day in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(day.date.formatted(date: .numeric, time: .omitted)).font(.headline)
                                Text("\(String(describing: day.levelUsed)) · сигналов: \(day.signalCount) · доход: \(money(day.dailyIncome))")
                                Text("Баланс: \(money(day.balanceAfter)) · следующий уровень: \(String(describing: day.levelForNextDay))")
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
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
            result = .init(date: end, days: days, signals: r.days.reduce(0) { $0 + $1.signalCount }, balance: r.expectedBalance, level: r.finalLevel, deposit: r.currentDeposit, gross: r.grossProfit, commission: r.withholding, net: r.netProfit, dayDetails: r.days)
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
        .init(date: r.reachedDate, days: r.daysCount, signals: r.totalSignals, balance: r.reachedBalance, level: r.finalLevel, deposit: r.currentDeposit, gross: r.grossProfit, commission: r.withholding, net: r.netProfit, dayDetails: r.days)
    }

    private func decimal(_ text: String) -> Decimal? {
        Decimal(string: text.replacingOccurrences(of: ",", with: "."))
    }

    private func formattedNumber(_ value: Decimal) -> String {
        let f = NumberFormatter()
        f.numberStyle = .decimal
        f.locale = Locale(identifier: "ru_RU")
        f.usesGroupingSeparator = true
        f.groupingSeparator = "\u{202F}"
        f.decimalSeparator = ","
        f.maximumFractionDigits = 2
        f.minimumFractionDigits = 0
        return f.string(from: value as NSDecimalNumber) ?? "\(value)"
    }

    private func rubles(_ value: Decimal) -> String {
        formattedNumber(value) + " ₽"
    }

    private func money(_ value: Decimal) -> String {
        formattedNumber(value) + " USDT"
    }
}
