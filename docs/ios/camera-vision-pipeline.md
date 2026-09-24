# Camera and Vision Pipeline

The capture path on iOS must produce an input tensor identical to what the model saw in
training. Any difference in crop, resize, or color handling shows up as wrong nutrition.

## Capture

- `AVCaptureSession` with a `.photo` preset and the back wide camera.
- Lock the orientation to portrait and present an overhead-framing overlay so users shoot
  the plate from above, matching Nutrition5k's overhead captures.
- Capture a still `AVCapturePhoto`, not a preview frame, for the final estimate (higher
  quality, less motion blur).

## Preprocessing (must match training)

1. Convert to `CVPixelBuffer` in RGB.
2. Center-crop to square, then resize to **300×300** for EfficientNetB3.
3. Apply the same scaling the Keras model used (see `coreml-conversion.md`). Prefer baking
   scale/bias into the Core ML `ImageType` so Vision handles it, avoiding a hand-written and
   error-prone Swift normalization.

## Running through Vision

```swift
let model = try VNCoreMLModel(for: ByteBite(configuration: .init()).model)
let request = VNCoreMLRequest(model: model) { request, _ in
    guard let obs = request.results?.first as? VNCoreMLFeatureValueObservation,
          let out = obs.featureValue.multiArrayValue else { return }
    // out holds 5 standardized values -> de-standardize -> NutritionResult
}
request.imageCropAndScaleOption = .centerCrop   // match training crop
let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .up)
try handler.perform([request])
```

## Gotchas

- `imageCropAndScaleOption` defaults to `.scaleFit`, which letterboxes and changes aspect
  ratio. Set it explicitly to whatever training used (center crop here).
- EXIF orientation: pass the correct `CGImagePropertyOrientation` or the plate arrives
  rotated and estimates drift.
- Run inference off the main thread; only hop back to update SwiftUI state.
