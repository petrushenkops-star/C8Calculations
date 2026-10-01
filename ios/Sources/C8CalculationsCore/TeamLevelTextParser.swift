import Foundation

public enum TeamLevelTextParser {
    public static func parse(_ raw: String) -> ParticipantLevel? {
        // Vision can return the same level with Latin/Cyrillic C, Unicode spaces
        // and punctuation between the prefix and digit. Normalize only the
        // characters that are known OCR variants, then match C1...C6.
        let normalized = raw
            .uppercased()
            .replacingOccurrences(of: "С", with: "C")
            .replacingOccurrences(of: "\u{00A0}", with: " ")

        guard let regex = try? NSRegularExpression(
            pattern: #"C[\s\-–—_:.,]*([1-6])(?![0-9])"#
        ) else { return nil }

        let range = NSRange(normalized.startIndex..<normalized.endIndex, in: normalized)
        guard let match = regex.firstMatch(in: normalized, range: range),
              let digitRange = Range(match.range(at: 1), in: normalized),
              let number = Int(normalized[digitRange]) else { return nil }

        return ParticipantLevel(rawValue: number - 1)
    }
}
