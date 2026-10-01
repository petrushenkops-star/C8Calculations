import SwiftUI
import PhotosUI
import C8CalculationsCore

struct TeamStructureView: View {
    @ObservedObject var store: TeamStore
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var recognitionStatus: String?
    @State private var recognizing = false

    var body: some View {
        Form {
            Section("Распознавание") {
                PhotosPicker(selection: $selectedPhoto, matching: .images) {
                    Label("Выбрать изображение структуры", systemImage: "photo")
                }
                if recognizing { ProgressView("Распознавание…") }
                if let recognitionStatus { Text(recognitionStatus).font(.footnote) }
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
            } footer: {
                Text("После распознавания значения можно исправить вручную. Если в самом левом столбце один распознанный блок, он считается лидером и исключается; если блоков несколько — лидер на изображении не показан.")
            }
        }
        .navigationTitle("Структура команды")
        .onChange(of: selectedPhoto) { _, item in
            guard let item else { return }
            Task { await recognize(item) }
        }
    }

    @MainActor
    private func recognize(_ item: PhotosPickerItem) async {
        recognizing = true; recognitionStatus = nil
        defer { recognizing = false }
        do {
            guard let data = try await item.loadTransferable(type: Data.self),
                  let image = UIImage(data: data) else {
                recognitionStatus = "Не удалось открыть изображение"; return
            }
            let result = try await VisionTeamRecognizer.recognize(image)
            for level in ParticipantLevel.allCases { store.set(level, result.counts[level, default: 0]) }
            recognitionStatus = result.leaderExcluded
                ? "Распознано блоков: \(result.recognizedBoxes). Лидер исключён."
                : "Распознано блоков: \(result.recognizedBoxes). Лидер не исключался."
        } catch {
            recognitionStatus = "Ошибка распознавания: \(error.localizedDescription)"
        }
    }
}
