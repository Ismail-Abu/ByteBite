# On-Device Inference and Performance

Goal: real-time-feeling estimates on iPhone with the model running on the Neural Engine
(ANE), fully offline.

## Compute units

Convert to an ML Program and let Core ML pick `.all`, which prefers the ANE and falls back
to GPU/CPU. For EfficientNetB3 at 300×300, expect single-digit-to-low-tens of milliseconds
on recent A-series chips once the model is warm.

## Warm-up

The first inference includes model load and compilation and is much slower than steady
state. Warm the model once at app launch with a dummy input so the user's first real photo
feels instant:

```swift
Task.detached(priority: .utility) {
    let dummy = try! MLMultiArray(shape: [1,300,300,3], dataType: .float32)
    _ = try? model.prediction(from: /* dummy featureProvider */)
}
```

## Profiling checklist

- Measure **cold** (first call) and **warm** (steady state) latency separately.
- Confirm the ANE is actually used via Instruments (Core ML template); if it silently falls
  back to CPU, latency and battery both suffer.
- Test on a real mid-range device (not just the newest Pro), since that is the honest
  experience for most users.
- Watch thermals: repeated captures should not throttle the ANE noticeably.

## Battery and thermals

Still-photo, on-demand inference is cheap — the model runs once per shutter press, not per
frame. Avoid running inference on the live preview stream; it wastes power for no accuracy
gain given the single-photo design.

## Numerical parity

Keep the Python-vs-Core ML MAE parity check from `coreml-conversion.md` in the loop. Device
inference is only useful if it produces the same nutrition numbers as the validated model.
