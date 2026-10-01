import SwiftUI
import PhotosUI

struct TeamImagePicker: View {
    @Binding var image: UIImage?

    var body: some View {
        PhotosPicker(selection: Binding(
            get: { nil },
            set: { item in
                guard let item else { return }
                Task {
                    if let data = try? await item.loadTransferable(type: Data.self),
                       let loaded = UIImage(data: data) { image = loaded }
                }
            }
        ), matching: .images) {
            Label("Выбрать изображение", systemImage: "photo")
        }
    }
}
