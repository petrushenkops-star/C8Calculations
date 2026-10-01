import XCTest
@testable import C8CalculationsCore

final class TeamLevelTextParserTests: XCTestCase {
    func testLatinAndCyrillicC() {
        XCTAssertEqual(TeamLevelTextParser.parse("C4"), .C4)
        XCTAssertEqual(TeamLevelTextParser.parse("С4"), .C4)
    }

    func testWhitespaceAndSeparators() {
        XCTAssertEqual(TeamLevelTextParser.parse(" C 5 "), .C5)
        XCTAssertEqual(TeamLevelTextParser.parse("С-6"), .C6)
        XCTAssertEqual(TeamLevelTextParser.parse("C:3"), .C3)
    }

    func testTextAroundLevel() {
        XCTAssertEqual(TeamLevelTextParser.parse("Уровень С2"), .C2)
        XCTAssertEqual(TeamLevelTextParser.parse("VIP C1"), .C1)
    }

    func testInvalidText() {
        XCTAssertNil(TeamLevelTextParser.parse("C7"))
        XCTAssertNil(TeamLevelTextParser.parse("C10"))
        XCTAssertNil(TeamLevelTextParser.parse("С60"))
        XCTAssertNil(TeamLevelTextParser.parse("LEVEL"))
    }
}
