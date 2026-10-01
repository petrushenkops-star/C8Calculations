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

        guard let regex = try? NSRegularExpression(pattern: #"C([1-6])(?![0-9])"#) else { return nil }
        let range = NSRange(normalized.startIndex..<normalized.endIndex, in: normalized)
        guard let match = regex.firstMatch(in: normalized, range: range),
              let digitRange = Range(match.range(at: 1), in: normalized),
              let number = Int(normalized[digitRange]) else { return nil }
        return ParticipantLevel(rawValue: number - 1)
    }
}
