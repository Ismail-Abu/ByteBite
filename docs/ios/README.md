# Bringing ByteBite to iPhone

ByteBite currently ships as an Android app running a TensorFlow Lite model on-device. This
folder plans the iOS port: same one-photo, zero-entry, fully-offline experience, native on
iPhone and iPad.

## Why iOS matters

- Reaches the large share of U.S. users on iPhone, where much of the health-tracking
  audience already lives.
- **HealthKit** integration lets ByteBite write carbohydrate and energy entries straight
  into Apple Health, so the estimate flows into the user's existing health record.
- The Neural Engine (ANE) on modern A- and M-series chips runs Core ML models with very low
  latency and power draw, which suits real-time capture.

## Port at a glance

| Layer | Android (today) | iOS (target) |
|---|---|---|
| Model runtime | TensorFlow Lite | Core ML (`.mlpackage`) |
| Camera | CameraX | AVFoundation / `AVCaptureSession` |
| Image prep | Bitmap + TFLite ops | Vision `VNImageRequest` / `vImage` |
| UI | Jetpack Compose | SwiftUI |
| Health export | (none yet) | HealthKit |

## Documents in this folder

1. `coreml-conversion.md` — converting the trained Keras/TFLite model to Core ML.
2. `swiftui-app-plan.md` — app architecture and screens.
3. `camera-vision-pipeline.md` — capture and preprocessing to match training.
4. `on-device-inference.md` — running and profiling the model on the Neural Engine.
5. `app-store-checklist.md` — HealthKit entitlements and submission requirements.

## Guiding principle

The iOS build must be **numerically faithful** to the Android/Python model: same input size,
same normalization, same de-standardization of outputs. Any drift there shows up as wrong
calorie and carb numbers, which is the one thing this app cannot get wrong.
