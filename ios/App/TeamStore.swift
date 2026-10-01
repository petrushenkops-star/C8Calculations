import Foundation
import SwiftUI
import C8CalculationsCore

@MainActor
final class TeamStore: ObservableObject {
    private let defaults = UserDefaults.standard
    @Published var counts: [ParticipantLevel: Int] {
        didSet { save() }
    }

    init() {
        var loaded: [ParticipantLevel: Int] = [:]
        for level in ParticipantLevel.allCases {
            loaded[level] = defaults.integer(forKey: "team.\(level.rawValue)")
        }
        counts = loaded
    }

    func count(_ level: ParticipantLevel) -> Int { counts[level, default: 0] }
    func set(_ level: ParticipantLevel, _ value: Int) { counts[level] = max(0, value) }
    var total: Int { counts.values.reduce(0, +) }

    private func save() {
        for level in ParticipantLevel.allCases {
            defaults.set(counts[level, default: 0], forKey: "team.\(level.rawValue)")
        }
    }
}
