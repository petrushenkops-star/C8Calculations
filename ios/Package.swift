// swift-tools-version: 6.0
import PackageDescription
let package = Package(
    name: "C8CalculationsCore",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [.library(name: "C8CalculationsCore", targets: ["C8CalculationsCore"])],
    targets: [
        .target(name: "C8CalculationsCore"),
        .testTarget(name: "C8CalculationsCoreTests", dependencies: ["C8CalculationsCore"])
    ]
)
