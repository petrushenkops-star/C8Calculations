import UIKit
@preconcurrency import Vision
import C8CalculationsCore

enum VisionTeamRecognizer {
    static func recognize(_ image: UIImage) async throws -> TeamRecognitionResult {
        guard let cgImage = image.cgImage else { throw RecognitionError.invalidImage }
        return try await withCheckedThrowingContinuation { continuation in
            let request = VNRecognizeTextRequest { request, error in
                if let error { continuation.resume(throwing: error); return }
                let observations = (request.results as? [VNRecognizedTextObservation]) ?? []
                let boxes = observations.compactMap { observation -> RecognizedTeamBox? in
                    guard let candidate = observation.topCandidates(1).first else { return nil }
                    return .init(text: candidate.string, minX: observation.boundingBox.minX, midY: observation.boundingBox.midY)
                }
                continuation.resume(returning: TeamStructureAnalyzer.analyze(boxes))
            }
            request.recognitionLevel = .accurate
            request.recognitionLanguages = ["ru-RU", "en-US"]
            request.usesLanguageCorrection = false
            do { try VNImageRequestHandler(cgImage: cgImage, orientation: .up).perform([request]) }
            catch { continuation.resume(throwing: error) }
        }
    }

    enum RecognitionError: Error { case invalidImage }
}
