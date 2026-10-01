import Foundation

public struct RecognizedTeamBox: Sendable {
    public let text: String
    public let minX: Double
    public let midY: Double

    public init(text: String, minX: Double, midY: Double) {
        self.text = text; self.minX = minX; self.midY = midY
    }
}

public struct TeamRecognitionResult: Sendable {
    public let counts: [ParticipantLevel: Int]
    public let leaderExcluded: Bool
    public let recognizedBoxes: Int
}

public enum TeamStructureAnalyzer {
    public static func level(from text: String) -> ParticipantLevel? {
        let normalized = text.uppercased()
            .replacingOccurrences(of: "С", with: "C")
            .replacingOccurrences(of: " ", with: "")
        for level in ParticipantLevel.allCases {
            if normalized.contains("C\(level.rawValue + 1)") { return level }
        }
        return nil
    }

    public static func analyze(_ boxes: [RecognizedTeamBox]) -> TeamRecognitionResult {
        let parsed = boxes.compactMap { box -> (RecognizedTeamBox, ParticipantLevel)? in
            guard let level = level(from: box.text) else { return nil }
            return (box, level)
        }
        var counts = Dictionary(uniqueKeysWithValues: ParticipantLevel.allCases.map { ($0, 0) })
        parsed.forEach { counts[$0.1, default: 0] += 1 }
        guard let leftX = parsed.map({ $0.0.minX }).min() else {
            return .init(counts: counts, leaderExcluded: false, recognizedBoxes: 0)
        }

        // Vision coordinates are normalized. Boxes close to the minimum X form the leftmost column.
        let tolerance = 0.035
        let leftColumn = parsed.filter { abs($0.0.minX - leftX) <= tolerance }
        let shouldExcludeLeader = leftColumn.count == 1
        if shouldExcludeLeader, let leaderLevel = leftColumn.first?.1 {
            counts[leaderLevel] = max(0, counts[leaderLevel, default: 0] - 1)
        }
        return .init(counts: counts, leaderExcluded: shouldExcludeLeader, recognizedBoxes: parsed.count)
    }
}
