import SwiftUI
import C8CalculationsCore

struct TeamStructureView: View {
    @ObservedObject var store: TeamStore

    var body: some View {
        Form {
            Section("Участники по уровням") {
                ForEach(ParticipantLevel.allCases, id: \.self) { level in
                    Stepper(value: Binding(get: { store.count(level) }, set: { store.set(level, $0) }), in: 0...9999) {
                        LabeledContent(String(describing: level), value: "\(store.count(level))")
                    }
                }
            }
            Section {
                LabeledContent("Всего участников", value: "\(store.total)")
            } footer: {
                Text("Лидер команды в количество участников не включается. После добавления OCR эти значения будут заполняться автоматически и останутся доступными для ручной корректировки.")
            }
        }
        .navigationTitle("Структура команды")
    }
}
