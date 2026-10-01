import Foundation
@preconcurrency import Vision
import UIKit
import ImageIO
import C8CalculationsCore

struct RecognizedTeamMember: Sendable {
    let level: ParticipantLevel
    let box: CGRect
}

enum TeamOCRService {
    static func recognize(image: UIImage) async throws -> [RecognizedTeamMember] {
        guard let cgImage = image.cgImage else { return [] }
        let orientation = cgImageOrientation(from: image.imageOrientation)

        return try await withCheckedThrowingContinuation { continuation in
            let request = VNRecognizeTextRequest { request, error in
                if let error { continuation.resume(throwing: error); return }
                let observations = (request.results as? [VNRecognizedTextObservation]) ?? []
                let members = observations.compactMap { observation -> RecognizedTeamMember? in
                    guard let text = observation.topCandidates(1).first?.string,
                          let level = TeamLevelTextParser.parse(text) else { return nil }
                    return RecognizedTeamMember(level: level, box: observation.boundingBox)
                }
                continuation.resume(returning: members)
            }
            request.recognitionLevel = .accurate
            request.usesLanguageCorrection = false
            request.recognitionLanguages = ["ru-RU", "en-US"]

            DispatchQueue.global(qos: .userInitiated).async {
                do {
                    try VNImageRequestHandler(
                        cgImage: cgImage,
                        orientation: orientation,
                        options: [:]
                    ).perform([request])
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    static func analyze(_ members: [RecognizedTeamMember]) -> TeamRecognitionResult {
        let boxes = members.map {
            RecognizedTeamBox(
                text: String(describing: $0.level),
                minX: Double($0.box.minX),
                midY: Double($0.box.midY),
                width: Double($0.box.width)
            )
        }
        return TeamStructureAnalyzer.analyze(boxes)
    }

    static func counts(from members: [RecognizedTeamMember]) -> [ParticipantLevel: Int] {
        analyze(members).counts
    }

    private static func cgImageOrientation(from orientation: UIImage.Orientation) -> CGImagePropertyOrientation {
        switch orientation {
        case .up: return .up
        case .upMirrored: return .upMirrored
        case .down: return .down
        case .downMirrored: return .downMirrored
        case .left: return .left
        case .leftMirrored: return .leftMirrored
        case .right: return .right
        case .rightMirrored: return .rightMirrored
        @unknown default: return .up
        }
    }
}
