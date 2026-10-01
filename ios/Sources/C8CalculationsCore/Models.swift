import Foundation

public enum ParticipantLevel: Int, CaseIterable, Codable, Sendable {
    case C1, C2, C3, C4, C5, C6
}

public struct LevelConfig: Sendable {
    public let level: ParticipantLevel
    public let deposit: Decimal
    public let incomePerSignal: Decimal
    public let baseSignalCount: Int
    public let fridaySignalCount: Int
    public let saturdaySignalCount: Int
}

public struct ProfitSimulationDay: Sendable {
    public let date: Date
    public let levelUsed: ParticipantLevel
    public let depositUsed: Decimal
    public let signalCount: Int
    public let incomePerSignal: Decimal
    public let dailyIncome: Decimal
    public let balanceAfter: Decimal
    public let levelForNextDay: ParticipantLevel
}

public struct ProfitSimulationResult: Sendable {
    public let days: [ProfitSimulationDay]
    public let expectedBalance: Decimal
    public let finalLevel: ParticipantLevel
    public let currentDeposit: Decimal
    public let grossProfit: Decimal
    public let withholding: Decimal
    public let netProfit: Decimal
}
