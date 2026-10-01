import Foundation

public enum LevelConfiguration {
    private static let configs: [ParticipantLevel: LevelConfig] = [
        .C1: .init(level: .C1, deposit: 300, incomePerSignal: Decimal(string: "2.4")!, baseSignalCount: 2, fridaySignalCount: 1, saturdaySignalCount: 1),
        .C2: .init(level: .C2, deposit: 700, incomePerSignal: Decimal(string: "5.6")!, baseSignalCount: 2, fridaySignalCount: 1, saturdaySignalCount: 1),
        .C3: .init(level: .C3, deposit: 1500, incomePerSignal: 12, baseSignalCount: 3, fridaySignalCount: 1, saturdaySignalCount: 1),
        .C4: .init(level: .C4, deposit: 3000, incomePerSignal: 24, baseSignalCount: 3, fridaySignalCount: 1, saturdaySignalCount: 1),
        .C5: .init(level: .C5, deposit: 6000, incomePerSignal: 48, baseSignalCount: 4, fridaySignalCount: 1, saturdaySignalCount: 1),
        .C6: .init(level: .C6, deposit: 10000, incomePerSignal: 80, baseSignalCount: 4, fridaySignalCount: 1, saturdaySignalCount: 1)
    ]
    public static func config(for level: ParticipantLevel) -> LevelConfig { configs[level]! }
}

public enum SignalRuleEngine {
    public static func signalCount(level: ParticipantLevel, date: Date, x: Int = 0, isVip: Bool = false, l1Count: Int = 0, calendar: Calendar = .current) -> Int {
        precondition(x >= 0 && l1Count >= 0)
        let c = LevelConfiguration.config(for: level)
        switch calendar.component(.weekday, from: date) {
        case 6: return c.fridaySignalCount + (l1Count >= 10 ? 1 : 0)
        case 7: return c.saturdaySignalCount + (l1Count >= 10 ? 1 : 0)
        default: return c.baseSignalCount + x + (isVip ? 1 : 0)
        }
    }
}

public enum LevelUpgradeEngine {
    public static func resolve(current: ParticipantLevel, balance: Decimal, enabled: Bool) -> ParticipantLevel {
        guard enabled else { return current }
        return ParticipantLevel.allCases
            .filter { LevelConfiguration.config(for: $0).deposit <= balance && $0.rawValue > current.rawValue }
            .max(by: { $0.rawValue < $1.rawValue }) ?? current
    }
}

public enum ProfitSimulationEngine {
    public static func simulate(startDate: Date, numberOfDays: Int, startingLevel: ParticipantLevel, startingBalance: Decimal, autoUpgrade: Bool = true, x: Int = 0, isVip: Bool = false, l1Count: Int = 0, calendar: Calendar = .current) -> ProfitSimulationResult {
        precondition(numberOfDays >= 0 && startingBalance >= 0)
        var level = startingLevel
        var balance = startingBalance
        var days: [ProfitSimulationDay] = []
        for index in 0..<numberOfDays {
            let date = calendar.date(byAdding: .day, value: index, to: startDate)!
            let config = LevelConfiguration.config(for: level)
            let signals = SignalRuleEngine.signalCount(level: level, date: date, x: x, isVip: isVip, l1Count: l1Count, calendar: calendar)
            let dailyIncome = config.incomePerSignal * Decimal(signals)
            let after = balance + dailyIncome
            let next = LevelUpgradeEngine.resolve(current: level, balance: after, enabled: autoUpgrade)
            days.append(.init(date: date, levelUsed: level, depositUsed: config.deposit, signalCount: signals, incomePerSignal: config.incomePerSignal, dailyIncome: dailyIncome, balanceAfter: after, levelForNextDay: next))
            balance = after
            level = next
        }
        let deposit = LevelConfiguration.config(for: level).deposit
        let gross = balance - deposit
        return .init(days: days, expectedBalance: balance, finalLevel: level, currentDeposit: deposit, grossProfit: gross, withholding: gross * Decimal(string: "0.30")!, netProfit: gross * Decimal(string: "0.70")!)
    }
}
