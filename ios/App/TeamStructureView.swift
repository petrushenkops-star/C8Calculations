import SwiftUI
import C8CalculationsCore

struct TeamStructureView: View {
    @ObservedObject var store: TeamStore
    @State private var image: UIImage?
    @State private var isRecognizing = false
    @State private var recognitionMessage: String?

    var body: some View {
        Form {
            Section("Распознавание") {
                TeamImagePicker(image: $image)
                if let image {
                    Image(uiImage: image).resizable().scaledToFit().frame(maxHeight: 220)
                    Button(isRecognizing ? "Распознавание…" : "Распознать структуру") {
                        recognize()
                    }.disabled(isRecognizing)
                }
                if let recognitionMessage { Text(recognitionMessage).font(.footnote) }
            }

            Section("Участники по уровням") {
                ForEach(ParticipantLevel.allCases, id: \.self) { level in
                    Stepper(value: Binding(get: { store.count(level) }, set: { store.set(level, $0) }), in: 0...9999) {
                        LabeledContent(String(describing: level), value: "\(store.count(level))")
                    }
                }
            }
            Section {
                LabeledContent("Всего участников", value: "\(store.total)")
                Button("Сбросить структуру", role: .destructive) {
                    for level in ParticipantLevel.allCases { store.set(level, 0) }
                    recognitionMessage = nil
                    image = nil
                }
            } footer: {
                Text("Если в самом левом столбце один прямоугольник, он считается лидером и исключается. Если слева несколько прямоугольников, лидер на изображении отсутствует. После OCR значения можно исправить вручную.")
            }
        }
        .navigationTitle("Структура команды")
    }

    private func recognize() {
        guard let image else { return }
        isRecognizing = true; recognitionMessage = nil
        Task {
            do {
                let members = try await TeamOCRService.recognize(image: image)
                let counts = TeamOCRService.counts(from: members)
                await MainActor.run {
                    for level in ParticipantLevel.allCases { store.set(level, counts[level, default: 0]) }
                    let details = ParticipantLevel.allCases.map { "\(String(describing: $0)): \(counts[$0, default: 0])" }.joined(separator: " · ")\n                    recognitionMessage = "Распознано: \(counts.values.reduce(0, +))\n\(details)"
                    isRecognizing = false
                }
            } catch {
                await MainActor.run {
                    recognitionMessage = "Не удалось распознать изображение"
                    isRecognizing = false
                }
            }
        }
    }
}
