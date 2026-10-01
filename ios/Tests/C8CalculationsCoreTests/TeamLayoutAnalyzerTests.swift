import XCTest
@testable import C8CalculationsCore

final class TeamLayoutAnalyzerTests: XCTestCase {
    func testSingleRectangleInLeftColumnIsExcludedAsLeader() {
        let members = [
            TeamMemberBox(level: .C5, midX: 0.10, width: 0.10),
            TeamMemberBox(level: .C4, midX: 0.35, width: 0.10),
            TeamMemberBox(level: .C4, midX: 0.35, width: 0.10)
        ]
        let counts = TeamLayoutAnalyzer.countsExcludingLeader(members)
        XCTAssertEqual(counts[.C5], 0)
        XCTAssertEqual(counts[.C4], 2)
    }

    func testSeveralRectanglesInLeftColumnMeansLeaderNotShown() {
        let members = [
            TeamMemberBox(level: .C3, midX: 0.10, width: 0.10),
            TeamMemberBox(level: .C4, midX: 0.11, width: 0.10),
            TeamMemberBox(level: .C5, midX: 0.40, width: 0.10)
        ]
        let counts = TeamLayoutAnalyzer.countsExcludingLeader(members)
        XCTAssertEqual(counts[.C3], 1)
        XCTAssertEqual(counts[.C4], 1)
        XCTAssertEqual(counts[.C5], 1)
    }
    func testLeftColumnToleratesSmallHorizontalOCRDrift() {
        let members = [
            TeamMemberBox(level: .C3, midX: 0.100, width: 0.10),
            TeamMemberBox(level: .C4, midX: 0.145, width: 0.10),
            TeamMemberBox(level: .C5, midX: 0.40, width: 0.10)
        ]
        XCTAssertNil(TeamLayoutAnalyzer.leaderIndex(in: members))
    }

    func testNextColumnIsNotMergedIntoLeftColumn() {
        let members = [
            TeamMemberBox(level: .C5, midX: 0.10, width: 0.10),
            TeamMemberBox(level: .C4, midX: 0.22, width: 0.10),
            TeamMemberBox(level: .C3, midX: 0.40, width: 0.10)
        ]
        XCTAssertEqual(TeamLayoutAnalyzer.leaderIndex(in: members), 0)
    }

    func testToleranceUsesTypicalWidthWhenBoxesDiffer() {
        let members = [
            TeamMemberBox(level: .C2, midX: 0.100, width: 0.08),
            TeamMemberBox(level: .C3, midX: 0.135, width: 0.10),
            TeamMemberBox(level: .C4, midX: 0.50, width: 0.12)
        ]
        XCTAssertNil(TeamLayoutAnalyzer.leaderIndex(in: members))
    }
}

