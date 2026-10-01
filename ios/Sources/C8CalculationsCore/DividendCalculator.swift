import Foundation

public struct DividendInput: Sendable {
    public let days, c1, c2, c3, c4, c5, c6: Int
    public init(days: Int, c1: Int = 0, c2: Int = 0, c3: Int = 0, c4: Int = 0, c5: Int = 0, c6: Int = 0) {
        self.days=days; self.c1=c1; self.c2=c2; self.c3=c3; self.c4=c4; self.c5=c5; self.c6=c6
    }
}
public struct DividendLevelResult: Sendable { public let level: String; public let participants: Int; public let baseAmount, amount: Decimal }
public struct DividendResult: Sendable { public let days: Int; public let levels: [DividendLevelResult]; public let total: Decimal }

public enum DividendCalculator {
    public static func calculate(_ input: DividendInput) -> DividendResult {
        precondition(input.days > 0)
        precondition([input.c1,input.c2,input.c3,input.c4,input.c5,input.c6].allSatisfy{$0 >= 0})
        let bases: [(String, Decimal, Int)] = [
            ("C2", Decimal(string:"11.2")!, input.c2), ("C3",24,input.c3), ("C4",48,input.c4), ("C5",96,input.c5), ("C6",160,input.c6)
        ]
        let levels = bases.map { level, base, count in
            DividendLevelResult(level: level, participants: count, baseAmount: base, amount: base * Decimal(string:"0.02")! * Decimal(input.days) * Decimal(count))
        }
        return .init(days: input.days, levels: levels, total: levels.reduce(0){$0+$1.amount})
    }
}
