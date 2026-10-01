import Foundation
import SwiftUI
import C8CalculationsCore

@MainActor
final class TeamStore: ObservableObject {
    @Published var counts: [ParticipantLevel: Int] = Dictionary(uniqueKeysWithValues: ParticipantLevel.allCases.map { ($0, 0) })

    func count(_ level: ParticipantLevel) -> Int { counts[level, default: 0] }
    func set(_ level: ParticipantLevel, _ value: Int) { counts[level] = max(0, value) }
    var total: Int { counts.values.reduce(0, +) }
}
