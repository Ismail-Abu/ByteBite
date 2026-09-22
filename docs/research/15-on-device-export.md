# 15. On-device export

## The goal

The trained v4 model is a 44 MB Keras file that ran on an L4 GPU. The app needs it running on an ordinary Android phone, offline, fast enough to feel instant, with the same answers the notebook gave. `notebooks/bytebite_android_export.ipynb` does this. It retrains nothing. It only converts, measures, gates, and writes the files the app needs.

## Why on-device at all

The obvious alternative is to send the photo to a server and run the model there. It was ruled out by requirement:

- **Privacy.** The input is a photo of someone's meal, and the output is health data. The app declares no `INTERNET` permission, so neither can leave the phone. That's enforced by Android, not by a promise.
- **Offline use.** A cafeteria basement or a flight still works.
- **No running costs.** No server to pay for, secure, or keep online.

## The routes that were weighed

| Route | Size | Verdict |
|---|---|---|
| LiteRT float32 | 44.4 MB | Reference only. Too big for what it adds over float16. |
| **LiteRT float16** | **22.3 MB** | **Shipped.** Half the size, error about 3e-4 in z-units (well under 1 kcal), and the only float format the GPU delegate accepts natively. |
| LiteRT int8 (full-integer, calibrated) | 13.0 MB | Gated. Ships only if it passes the accuracy rule below. |
| LiteRT dynamic-range | ~13 MB | Dropped. int8-sized, but slower than full-integer and less accurate than float16. It loses on both axes. |
| int8 with quantization-aware training | ~13 MB | Deferred. That's a training change, not a conversion. |
| ONNX Runtime Mobile | 22-44 MB | Dropped. An extra toolchain step and a second numerical surface to validate, for no gain on an Android-only TensorFlow model. |
| ExecuTorch / PyTorch Mobile | n/a | Dropped. It would mean re-implementing and retraining the model. |
| MediaPipe Tasks | n/a | Dropped. Its vision tasks cover classification and detection, not 5-output regression. |
| EfficientNetV2B0 at 180 px | ~7 MB | Dropped. Carb MAE 7.42 g against 4.12 g ([07](07-model-choice.md)). |

LiteRT is the current name for TensorFlow Lite. Since the model was trained in TensorFlow, it's the shortest path with the fewest places for numbers to drift.

## What quantization does

A float32 weight uses 32 bits. Quantization stores weights with fewer:

- **float16** uses 16 bits per weight. The file is half the size, precision drops slightly, and the error is negligible for this model.
- **int8** uses 8 bits per weight, plus a scale and zero-point per tensor, so the file is 4x smaller than float32. Full-integer int8 needs a **calibration set** to learn the range of each layer's activations. The notebook uses 200 images from the **train** split only, since calibrating on test data would leak it.

For a classifier, int8 rounding rarely changes the top class. For a regressor that outputs grams, every bit of rounding moves the answer directly. That's why int8 isn't assumed to be safe here.

## The pre-registered int8 gate

Stated in Cell 1, before anything is measured:

- float16 is the default.
- int8 replaces it only if, on the full test set, **(a)** every nutrient's MAE degrades by less than 2% relative to float32, **and (b)** carb MAE degrades by less than 0.10 g in absolute terms.

Rule (b) is deliberately stricter than rule (a). Carbs drive insulin dosing ([03](03-problem.md)), so a 4x smaller file isn't worth a worse carb estimate. The README records the outcome: float16 ships.

Writing the rule down first is the same discipline as the validation gate ([11](11-validation-gate.md)). It stops a small accuracy loss from being argued away because the file is so much smaller.

## Delegates: where the math runs

- **GPU delegate.** Used when the model is float and the phone's driver supports it. If anything fails, it falls back to CPU. An accelerator must never be able to crash the app.
- **NNAPI.** Deliberately not used. It's deprecated as of Android 15, and the CPU path is more dependable for this graph.
- **CPU (XNNPACK, 4 threads).** The floor that always works.

A counter-intuitive measurement: on a desktop CPU, float16 was slower than float32 (47 ms against 41 ms), because float16 weights are converted back to float32 on each operation. float16's wins are file size and GPU eligibility, not CPU speed. Host timings rank the variants but say nothing reliable about a phone. The app times every inference and displays it (`on-device · float16 · <n> ms`), and that's the number to quote.

## APK size

With float16 in assets, the debug APK measured 66 MB. The LiteRT native libraries ship for all four CPU architectures, and three of them account for 22.7 MB. Every current Android phone is arm64, so restricting to `arm64-v8a` would take the app to about 44 MB. That's fine for a sideloaded conference demo, and the first thing to change for real distribution.

## What gets written to the app

| File | Why it exists |
|---|---|
| `bytebite_v4.tflite` | the gated model, the only file that does arithmetic |
| `bytebite_model.json` | input range, geometry, target order, train-only mu and sd |
| `fixture_dish.png` + `.json` | one pinned test dish and the notebook's answer for it |

The JSON sidecar keeps the preprocessing contract in one place. If a future model trains at a different resolution or refits the scaler, the JSON changes and the Kotlin picks it up without code edits. The fixture is checked by `ModelFixtureTest` on the device. If the phone's answer ever disagrees with the notebook's, the test fails loudly. With no model installed, it skips instead of failing.

## Check yourself

<details>
<summary>Why calibrate int8 on train images rather than test images?</summary>

Calibration sets the activation ranges baked into the model. Using test images would let the test set influence the shipped model, which is a leak. It would also make the int8 accuracy check look better than it should.
</details>

<details>
<summary>float16 was slower than float32 on the desktop CPU. Why ship it anyway?</summary>

Its benefits are file size (half) and GPU delegate eligibility, and on a phone with a supported GPU the delegate is where the speed comes from. Desktop CPU timings don't predict phone performance, so the app measures latency on the device itself.
</details>
