# Running ByteBite's model on the phone

The v4 EfficientNetB3 regressor is a 44 MB Keras model trained on an L4. This
document covers how it gets onto an Android device, which conversion routes were
considered, and why the shipped one was chosen.

Inference is **entirely local**. The app declares no `INTERNET` permission, so no
photo and no estimate can leave the device — worth keeping for a tool whose input
is a picture of someone's meal and whose output is health data.

---

## Routes considered

Sizes and host timings below are measured on this architecture at 300×300.
Accuracy columns come from the export notebook's test-set evaluation.

| Route | Size | Works for this model? | Verdict |
|---|---|---|---|
| **LiteRT float32** | 44.4 MB | yes, exact | Reference. Ships nothing — too big for the accuracy it adds over fp16. |
| **LiteRT float16** | 22.3 MB | yes | **Shipped.** Half the size, error ~3e-4 in z units (well under a single kcal), and the only float format the GPU delegate takes natively. |
| **LiteRT int8, full-integer PTQ** | 13.0 MB | yes, with a calibration set | Gated, not assumed. 4× smaller and the fastest CPU path, but quantizing a 5-output regressor costs real grams — the notebook measures it and only ships int8 if carbohydrate MAE degrades by < 0.10 g. |
| LiteRT dynamic-range quant | ~13 MB | yes, no calibration set needed | Dropped: int8-sized but slower than full-integer and less accurate than fp16, so it loses on both axes. |
| int8 with quantization-aware training | ~13 MB | would need retraining | Deferred. The honest way to recover int8 accuracy, but it is a training change, not a conversion one. |
| ONNX Runtime Mobile | ~22–44 MB | yes, via `tf2onnx` | Dropped: an extra toolchain hop and a second numerical surface to validate, for no gain on an Android-only, TF-trained model. Reconsider if an iOS build is ever wanted from one artifact. |
| ExecuTorch / PyTorch Mobile | n/a | needs a PyTorch port | Dropped. Re-implementing and re-training the model to change runtimes is not a conversion strategy. |
| MediaPipe Tasks | n/a | no matching task | Dropped: the vision tasks cover classification and detection, not 5-output regression, so it would mean a custom graph with no benefit over calling LiteRT directly. |
| Swap to EfficientNetV2B0 @180px | ~7 MB | already trained | Dropped on the paper's own numbers: carbohydrate MAE 7.42 g vs 4.12 g. A 3× smaller file is not worth an 80% worse carb estimate in a diabetes tool. |
| Server-side inference | 0 on device | yes | Out of scope by requirement, and it would add an `INTERNET` permission plus a meal-photo upload path. |

### Delegates

- **GPU delegate** — used when the shipped variant is float and the driver reports
  support. Falls back to CPU on any failure; an accelerator must never be able to
  take the app down.
- **NNAPI** — deliberately not used. It is deprecated as of Android 15, and on this
  graph the XNNPACK CPU path is the dependable baseline.
- **CPU / XNNPACK** — the floor, 4 threads.

A note on fp16 and CPU: on a host x86 core fp16 measured *slower* than fp32
(47 ms vs 41 ms), because fp16 weights are dequantized per-op on CPU. Its wins are
file size and GPU eligibility, not CPU speed. Expect the same ordering on a phone
CPU, and the reverse once the GPU delegate engages.

### APK size

The two LiteRT native libraries ship for all four ABIs, which is ~23 MB of the
debug APK before the model is added — so a `float16` build lands near 60 MB. For a
sideloaded poster demo that is fine. For anything distributed, restrict the ABIs:

```kotlin
// app/build.gradle.kts, inside defaultConfig
ndk { abiFilters += listOf("arm64-v8a") }
```

Every current Android phone is arm64, and dropping the other three takes roughly
16 MB off. Keep `x86_64` too if the emulator matters for development.

---

## Installing the model

The `.tflite` is not committed (see `app/src/main/assets/README.md`). On the
machine holding the trained weights:

```
NUTRITION5K_DIR=/path/to/nutrition5k_dataset \
BYTEBITE_V4_MODEL=outputs/v4_s42.keras \
BYTEBITE_ANDROID_ASSETS=android/app/src/main/assets \
jupyter nbconvert --to notebook --execute notebooks/bytebite_android_export.ipynb
```

Then verify the phone reproduces the notebook:

```
cd android && ./gradlew connectedDebugAndroidTest
```

`ModelFixtureTest` runs the shipped model on a pinned Nutrition5k test dish and
asserts the on-device answer matches what the notebook measured for that dish.
With no model installed the test **skips** rather than fails.

## The three things that break this port

All three are pinned in `bytebite_model.json` and enforced by the fixture test.

1. **Input range is 0–255, not 0–1.** `keras.applications.EfficientNetB3` carries
   its `Rescaling` and `Normalization` layers *inside* the graph, and
   `efficientnet.preprocess_input` is a no-op. A `/255f` in the Kotlin
   double-normalizes the input and returns confident nonsense rather than an
   obvious error.
2. **Outputs are z-scores.** The head emits five standardized values;
   `real = z * sd + mu` using **train-only** statistics, index-aligned with
   `targets` — which is `[calories, mass, fat, carb, protein]`, not alphabetical.
3. **Geometry must match training.** Training stretched the full overhead frame to
   300×300 with no crop. `NutritionEstimator.prepare` centre-crops to the
   dataset's aspect ratio and then stretches, so a 4:3 phone photo reproduces the
   training distortion. A square centre-crop would shrink the apparent portion and
   bias mass and calories low.

## Measuring on-device latency

Every `Estimate` carries its own `latencyMs`, timed around `interpreter.run`, and
the result screens print it (`on-device · float16 · 180 ms`). That is the number to
quote — the notebook's host timings rank the variants but say nothing about a
phone. Inference runs on `Dispatchers.Default`, serialised by a mutex, because a
LiteRT `Interpreter` cannot be invoked concurrently.

## What the UI can and cannot claim

Two elements of the original mockups describe outputs this model does not have,
and they are now shown only in the sample-data state:

- the **per-ingredient carb split** ("≈38 g from rice") — v4 has one `Dense(5)`
  head over the whole dish and no per-ingredient output;
- the **cooking-cue / hidden-sugar read** — v4 dropped the text and
  cooking-classification heads permanently.

A live estimate shows the model's measured test error instead, which is
information it actually has.

## Known gaps

- **No depth.** Nutrition5k ships overhead depth that v4 ignores, and mass is the
  weakest target. Using it means a second input tensor and a retrained model, not
  a conversion change.
- **Overhead framing is advisory.** Capture goes through the system camera app, so
  the app can ask for a flat overhead shot but cannot enforce it. A study
  deployment would want CameraX so the pose can be checked before the shutter
  fires.
- **Thermals over a long session are unmeasured.** EfficientNetB3 at 300×300 is the
  heaviest of the three models in the paper; sustained scanning behaviour needs
  device testing.
