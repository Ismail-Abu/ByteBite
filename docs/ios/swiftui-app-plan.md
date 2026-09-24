# SwiftUI App Plan

A minimal, three-screen iOS app that mirrors the Android experience: point, shoot, read the
numbers.

## Screens

1. **Capture** — full-screen camera viewfinder with an overhead-framing guide and a single
   shutter button. Matches the Android live viewfinder.
2. **Result** — the photo with the five estimates (calories, carbs, protein, fat, mass) and
   a confidence note. A "Save to Health" button writes to HealthKit.
3. **History** — a simple list of past estimates, stored locally.

## Architecture

- **SwiftUI** for all views, `@Observable` view models.
- **AVFoundation** capture session owned by a `CameraModel`.
- **Core ML + Vision** inference in a `NutritionEstimator` service.
- **SwiftData** (or Core Data) for local history. No server, no account.

```
CaptureView ─┐
             ├─> CameraModel ──> frame ──> NutritionEstimator ──> NutritionResult
ResultView ──┘                                   │
                                                 └─> HealthKitWriter (opt-in)
HistoryView ──> ResultStore (SwiftData)
```

## Data model

```swift
struct NutritionResult: Identifiable, Codable {
    let id: UUID
    let date: Date
    let calories: Double   // kcal
    let mass: Double       // grams
    let carbs: Double      // grams
    let protein: Double    // grams
    let fat: Double        // grams
    let imageThumbnail: Data
}
```

## Principles

- Offline-first. The app must fully function in airplane mode.
- No account, no upload. Privacy is a feature, not a setting.
- One-tap flow. From launch to reading numbers should be two taps: open, shoot.
