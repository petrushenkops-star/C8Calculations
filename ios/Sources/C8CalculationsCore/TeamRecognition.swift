import Foundation

public struct RecognizedTeamBox: Sendable {
    public let text: String
    public let minX: Double
    public let midY: Double
    public let width: Double

    public init(text: String, minX: Double, midY: Double, width: Double = 0.10) {
        self.text = text
        self.minX = minX
        self.midY = midY
        self.width = width
    }
}

public struct TeamRecognitionResult: Sendable {
    public let counts: [ParticipantLevel: Int]
    public let leaderExcluded: Bool
    public let recognizedBoxes: Int
}

public enum TeamStructureAnalyzer {
    public static func level(from text: String) -> ParticipantLevel? {
        TeamLevelTextParser.parse(text)
    }

    public static func analyze(_ boxes: [RecognizedTeamBox]) -> TeamRecognitionResult {
        let parsed = boxes.compactMap { box -> (RecognizedTeamBox, ParticipantLevel)? in
            guard let level = TeamLevelTextParser.parse(box.text) else { return nil }
            return (box, level)
        }

        let members = parsed.map { box, level in
            TeamMemberBox(level: level, midX: box.minX + box.width / 2, width: box.width)
        }
        let leaderIndex = TeamLayoutAnalyzer.leaderIndex(in: members)
        let counts = TeamLayoutAnalyzer.countsExcludingLeader(members)

        return .init(
            counts: counts,
            leaderExcluded: leaderIndex != nil,
            recognizedBoxes: parsed.count
        )
    }
}
