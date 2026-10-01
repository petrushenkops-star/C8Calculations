import XCTest
@testable import C8CalculationsCore

final class TeamRecognitionTests: XCTestCase {
    func testAcceptsLatinAndCyrillicC() {
        XCTAssertEqual(TeamStructureAnalyzer.level(from: "C4"), .C4)
        XCTAssertEqual(TeamStructureAnalyzer.level(from: "С4"), .C4)
        XCTAssertEqual(TeamStructureAnalyzer.level(from: "уровень С6"), .C6)
    }

    func testSingleBoxInLeftmostColumnIsExcludedAsLeader() {
        let boxes = [
            RecognizedTeamBox(text: "C5", minX: 0.05, midY: 0.5),
            RecognizedTeamBox(text: "C3", minX: 0.25, midY: 0.7),
            RecognizedTeamBox(text: "С3", minX: 0.25, midY: 0.3)
        ]
        let result = TeamStructureAnalyzer.analyze(boxes)
        XCTAssertTrue(result.leaderExcluded)
        XCTAssertEqual(result.counts[.C5], 0)
        XCTAssertEqual(result.counts[.C3], 2)
    }

    func testSeveralBoxesInLeftmostColumnMeansLeaderNotShown() {
        let boxes = [
            RecognizedTeamBox(text: "C4", minX: 0.05, midY: 0.7),
            RecognizedTeamBox(text: "С4", minX: 0.052, midY: 0.3),
            RecognizedTeamBox(text: "C6", minX: 0.30, midY: 0.5)
        ]
        let result = TeamStructureAnalyzer.analyze(boxes)
        XCTAssertFalse(result.leaderExcluded)
        XCTAssertEqual(result.counts[.C4], 2)
        XCTAssertEqual(result.counts[.C6], 1)
    }
}
