import Foundation

public struct TargetCalculationResult: Sendable {
    public let reachedDate: Date
    public let daysCount: Int
    public let reachedAfterSignal: Int
    public let totalSignals: Int
    public let totalIncome: Decimal
    public let reachedBalance: Decimal
    public let finalLevel: ParticipantLevel
    public let currentDeposit: Decimal
    public let grossProfit: Decimal
    public let withholding: Decimal
    public let netProfit: Decimal
    public let days: [ProfitSimulationDay]
}

private let withholdingRate = Decimal(string: "0.30")!
private let netRate = Decimal(string: "0.70")!
private let maxDays = 36500

private func dayCalculation(date: Date, level: ParticipantLevel, balance: Decimal, x: Int, isVip: Bool, l1Count: Int, autoUpgrade: Bool, calendar: Calendar) -> ProfitSimulationDay {
    let config = LevelConfiguration.config(for: level)
    let signals = SignalRuleEngine.signalCount(level: level, date: date, x: x, isVip: isVip, l1Count: l1Count, calendar: calendar)
    let income = config.incomePerSignal * Decimal(signals)
    let after = balance + income
    let next = LevelUpgradeEngine.resolve(current: level, balance: after, enabled: autoUpgrade)
    return .init(date: date, levelUsed: level, depositUsed: config.deposit, signalCount: signals, incomePerSignal: config.incomePerSignal, dailyIncome: income, balanceAfter: after, levelForNextDay: next)
}

public enum TargetBalanceCalculator {
    public static func calculate(startDate: Date, startingLevel: ParticipantLevel, startingBalance: Decimal, targetBalance: Decimal, autoUpgrade: Bool = true, x: Int = 0, isVip: Bool = false, l1Count: Int = 0, calendar: Calendar = .current) -> TargetCalculationResult {
        precondition(startingBalance >= 0 && targetBalance >= 0 && x >= 0 && l1Count >= 0)
        var level = startingLevel, balance = startingBalance, date = startDate
        var days: [ProfitSimulationDay] = []
        while balance < targetBalance {
            precondition(days.count < maxDays)
            let day = dayCalculation(date: date, level: level, balance: balance, x: x, isVip: isVip, l1Count: l1Count, autoUpgrade: autoUpgrade, calendar: calendar)
            days.append(day); balance = day.balanceAfter; level = day.levelForNextDay
            if balance < targetBalance { date = calendar.date(byAdding: .day, value: 1, to: date)! }
        }
        let signal: Int
        let reached: Decimal
        if let last = days.last {
            let before = last.balanceAfter - last.dailyIncome
            let missing = targetBalance - before
            signal = max(1, min(last.signalCount, Int(NSDecimalNumber(decimal: missing / last.incomePerSignal).rounding(accordingToBehavior: RoundUpBehavior()).intValue)))
            reached = before + last.incomePerSignal * Decimal(signal)
        } else { signal = 0; reached = balance }
        return makeResult(startDate: startDate, days: days, signal: signal, reached: reached, level: level)
    }
}

public enum TargetNetProfitCalculator {
    public static func calculate(startDate: Date, startingLevel: ParticipantLevel, startingBalance: Decimal, targetNetProfit: Decimal, autoUpgrade: Bool = true, x: Int = 0, isVip: Bool = false, l1Count: Int = 0, calendar: Calendar = .current) -> TargetCalculationResult {
        precondition(startingBalance >= 0 && targetNetProfit >= 0 && x >= 0 && l1Count >= 0)
        var level = startingLevel, balance = startingBalance, date = startDate
        var days: [ProfitSimulationDay] = []
        func net(_ b: Decimal, _ l: ParticipantLevel) -> Decimal { (b - LevelConfiguration.config(for: l).deposit) * netRate }
        if net(balance, level) >= targetNetProfit { return makeResult(startDate: startDate, days: [], signal: 0, reached: balance, level: level) }
        while true {
            precondition(days.count < maxDays)
            let full = dayCalculation(date: date, level: level, balance: balance, x: x, isVip: isVip, l1Count: l1Count, autoUpgrade: autoUpgrade, calendar: calendar)
            var signalBalance = balance
            for signal in 1...full.signalCount {
                signalBalance += full.incomePerSignal
                let profitLevel = signal == full.signalCount ? full.levelForNextDay : level
                if net(signalBalance, profitLevel) >= targetNetProfit {
                    let partial = ProfitSimulationDay(date: date, levelUsed: level, depositUsed: full.depositUsed, signalCount: signal, incomePerSignal: full.incomePerSignal, dailyIncome: full.incomePerSignal * Decimal(signal), balanceAfter: signalBalance, levelForNextDay: profitLevel)
                    return makeResult(startDate: startDate, days: days + [partial], signal: signal, reached: signalBalance, level: profitLevel)
                }
            }
            days.append(full); balance = full.balanceAfter; level = full.levelForNextDay
            date = calendar.date(byAdding: .day, value: 1, to: date)!
        }
    }
}

private func makeResult(startDate: Date, days: [ProfitSimulationDay], signal: Int, reached: Decimal, level: ParticipantLevel) -> TargetCalculationResult {
    let deposit = LevelConfiguration.config(for: level).deposit
    let gross = reached - deposit
    let previousDays = signal > 0 ? Array(days.dropLast()) : days
    let totalSignals = previousDays.reduce(0) { $0 + $1.signalCount } + signal
    let totalIncome = previousDays.reduce(Decimal.zero) { $0 + $1.dailyIncome } + (signal > 0 ? (days.last?.incomePerSignal ?? 0) * Decimal(signal) : 0)
    return .init(reachedDate: days.last?.date ?? startDate, daysCount: days.count, reachedAfterSignal: signal, totalSignals: totalSignals, totalIncome: totalIncome, reachedBalance: reached, finalLevel: level, currentDeposit: deposit, grossProfit: gross, withholding: gross * withholdingRate, netProfit: gross * netRate, days: days)
}

private final class RoundUpBehavior: NSObject, NSDecimalNumberBehaviors {
    func roundingMode() -> Decimal.RoundingMode { .up }
    func scale() -> Int16 { 0 }
    func exceptionDuringOperation(_ operation: Selector, error: Decimal.CalculationError, leftOperand: NSDecimalNumber, rightOperand: NSDecimalNumber?) -> NSDecimalNumber? { nil }
}
