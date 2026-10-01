import XCTest
@testable import C8CalculationsCore

final class TargetCalculationTests: XCTestCase {
    private var utcCalendar: Calendar {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(secondsFromGMT: 0)!
        return c
    }
    private func date(_ text: String) -> Date {
        let f = DateFormatter(); f.calendar = utcCalendar; f.timeZone = utcCalendar.timeZone; f.dateFormat = "yyyy-MM-dd"
        return f.date(from: text)!
    }

    func testTargetNetProfitMatchesAcceptedAndroidScenario() {
        let result = TargetNetProfitCalculator.calculate(
            startDate: date("2026-09-30"), startingLevel: .C5, startingBalance: 6000,
            targetNetProfit: 3000, x: 1, isVip: true, l1Count: 0, calendar: utcCalendar
        )
        XCTAssertEqual(result.reachedDate, date("2026-10-29"))
        XCTAssertEqual(result.daysCount, 30)
        XCTAssertEqual(result.reachedAfterSignal, 4)
        XCTAssertEqual(result.totalSignals, 138)
        XCTAssertEqual(result.totalIncome, 8352)
        XCTAssertEqual(result.reachedBalance, 14352)
        XCTAssertEqual(result.finalLevel, .C6)
        XCTAssertEqual(result.currentDeposit, 10000)
        XCTAssertEqual(result.grossProfit, 4352)
        XCTAssertEqual(result.withholding, Decimal(string: "1305.6")!)
        XCTAssertEqual(result.netProfit, Decimal(string: "3046.4")!)
    }

    func testTargetBalanceStopsInsideDay() {
        let result = TargetBalanceCalculator.calculate(
            startDate: date("2026-09-30"), startingLevel: .C5, startingBalance: 6000,
            targetBalance: 6200, x: 1, isVip: true, l1Count: 0, calendar: utcCalendar
        )
        XCTAssertEqual(result.daysCount, 1)
        XCTAssertEqual(result.reachedAfterSignal, 5)
        XCTAssertEqual(result.reachedBalance, 6240)
    }
    func testDateModeProcessesWholeFinalDayUnlikeTargetMode() {
        let result = ProfitSimulationEngine.simulate(
            startDate: date("2026-09-30"),
            numberOfDays: 30,
            startingLevel: .C5,
            startingBalance: 6000,
            x: 1,
            isVip: true,
            l1Count: 0,
            calendar: utcCalendar
        )
        XCTAssertEqual(result.expectedBalance, 14512)
        XCTAssertEqual(result.finalLevel, .C6)
        XCTAssertEqual(result.currentDeposit, 10000)
    }
}

