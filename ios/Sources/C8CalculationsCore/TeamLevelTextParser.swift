import Foundation

public enum TeamLevelTextParser {
    public static func parse(_ raw: String) -> ParticipantLevel? {
        var normalized = raw.uppercased()
            .replacingOccurrences(of: "С", with: "C")
            .replacingOccurrences(of: " ", with: "")
            .replacingOccurrences(of: "\n", with: "")
            .replacingOccurrences(of: "\t", with: "")
        for separator in ["-", "_", ":", ".", ","] {
            normalized = normalized.replacingOccurrences(of: separator, with: "")
        }
        for level in ParticipantLevel.allCases {
            let token = "C\(level.rawValue + 1)"
            if normalized.contains(token) { return level }
        }
        return nil
    }
}
