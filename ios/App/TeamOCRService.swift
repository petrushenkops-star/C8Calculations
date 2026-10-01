import Foundation
@preconcurrency import Vision
import UIKit
import C8CalculationsCore

struct RecognizedTeamMember: Sendable {
    let level: ParticipantLevel
    let box: CGRect
}

enum TeamOCRService {
    static func recognize(image: UIImage) async throws -> [RecognizedTeamMember] {
        guard let cgImage = image.cgImage else { return [] }
        return try await withCheckedThrowingContinuation { continuation in
            let request = VNRecognizeTextRequest { request, error in
                if let error { continuation.resume(throwing: error); return }
                let observations = (request.results as? [VNRecognizedTextObservation]) ?? []
                let members = observations.compactMap { observation -> RecognizedTeamMember? in
                    guard let text = observation.topCandidates(1).first?.string,
                          let level = level(from: text) else { return nil }
                    return RecognizedTeamMember(level: level, box: observation.boundingBox)
                }
                continuation.resume(returning: members)
            }
            request.recognitionLevel = .accurate
            request.usesLanguageCorrection = false
            request.recognitionLanguages = ["ru-RU", "en-US"]
            DispatchQueue.global(qos: .userInitiated).async {
                do { try VNImageRequestHandler(cgImage: cgImage).perform([request]) }
                catch { continuation.resume(throwing: error) }
            }
        }
    }

    static func counts(from members: [RecognizedTeamMember]) -> [ParticipantLevel: Int] {
        let boxes = members.map {
            TeamMemberBox(level: $0.level, midX: Double($0.box.midX), width: Double($0.box.width))
        }
        return TeamLayoutAnalyzer.countsExcludingLeader(boxes)
    }

    private static func level(from raw: String) -> ParticipantLevel? {
        let normalized = raw.uppercased()
            .replacingOccurrences(of: "С", with: "C")
            .replacingOccurrences(of: " ", with: "")
        for level in ParticipantLevel.allCases {
            if normalized.range(of: "C\\s*\(level.rawValue + 1)", options: .regularExpression) != nil { return level }
        }
        return nil
    }
}
