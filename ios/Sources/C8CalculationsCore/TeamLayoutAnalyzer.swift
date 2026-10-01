import Foundation

public struct TeamMemberBox: Sendable {
    public let level: ParticipantLevel
    public let midX: Double
    public let width: Double
    public init(level: ParticipantLevel, midX: Double, width: Double) {
        self.level = level; self.midX = midX; self.width = width
    }
}

public enum TeamLayoutAnalyzer {
    public static func leaderIndex(in members: [TeamMemberBox]) -> Int? {
        guard !members.isEmpty else { return nil }
        let minX = members.map(\.midX).min()!
        let widths = members.map(\.width).sorted()
        let tolerance = max(widths[widths.count / 2] * 0.65, 0.025)
        let left = members.indices.filter { abs(members[$0].midX - minX) <= tolerance }
        return left.count == 1 ? left[0] : nil
    }

    public static func countsExcludingLeader(_ members: [TeamMemberBox]) -> [ParticipantLevel: Int] {
        let leader = leaderIndex(in: members)
        var counts = Dictionary(uniqueKeysWithValues: ParticipantLevel.allCases.map { ($0, 0) })
        for index in members.indices where index != leader { counts[members[index].level, default: 0] += 1 }
        return counts
    }
}
