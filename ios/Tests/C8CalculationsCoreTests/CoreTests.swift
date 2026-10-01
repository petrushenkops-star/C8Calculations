import XCTest
@testable import C8CalculationsCore

final class CoreTests: XCTestCase {
    func testCurrentTeamDividendMatchesAndroid() {
        let result = DividendCalculator.calculate(.init(days: 10, c1: 1, c2: 0, c3: 6, c4: 10, c5: 6, c6: 6))
        XCTAssertEqual(result.levels.reduce(0) { $0 + $1.participants }, 28)
        XCTAssertEqual(result.total, 432)
        XCTAssertEqual(result.total, DividendCalculator.calculate(.init(days: 10, c1: 0, c2: 0, c3: 6, c4: 10, c5: 6, c6: 6)).total)
    }

    func testLevelConfigurationMatchesAndroid() {
        XCTAssertEqual(LevelConfiguration.config(for: .C5).deposit, 6000)
        XCTAssertEqual(LevelConfiguration.config(for: .C6).incomePerSignal, 80)
    }
}
