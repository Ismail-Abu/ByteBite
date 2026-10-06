# 16. Port pitfalls and next steps

## The three silent bugs

Porting a regression model to a phone has a nasty property: most mistakes don't crash. They produce plausible numbers that are wrong. All three below are pinned in `bytebite_model.json` and checked by the on-device fixture test.

### 1. Input range is 0-255, not 0-1

Most image pipelines scale pixels to 0-1 by dividing by 255. Keras' `EfficientNetB3` builds its own `Rescaling` and `Normalization` layers into the model, and `efficientnet.preprocess_input` does nothing. So the exported model expects raw RGB values from 0 to 255.

A `/255f` in the Kotlin normalizes twice. The model then sees an almost black image. It doesn't error. It returns a confident estimate for a dark plate. This is the most common bug in this kind of port, and the fixture test catches it immediately, because the phone's answer for the pinned dish won't match the notebook's.

### 2. Know whether the head is standardized

The v4 head outputs z-scores, so the phone must compute `real = z * sd + mu` with the train-only statistics, in the order `[calories, mass, fat, carb, protein]`. The older `model1_control` head outputs raw kcal and grams.

If you treat a z-score head as raw, "carbs 0.4" gets shown as 0.4 g. If you treat a raw head as z-scores, you get thousands of grams. The export notebook checks which reading matches the labels instead of assuming. For a raw head it writes `mu = 0` and `sd = 1`, so the phone's one line of arithmetic works for both. If the target order is wrong, fat and carbs swap, and nothing looks broken until you compare with the labels.

### 3. Geometry must match training

Training stretched the full 4:3 overhead frame to 300x300 with no crop. The phone:

1. rotates a portrait photo to landscape (the model is rotation-invariant thanks to augmentation, [10](10-resolution-and-augmentation.md))
2. centre-crops to the dataset's measured aspect ratio
3. stretches to 300x300

The tempting alternative, a square centre-crop, cuts off the sides of the plate and changes how large the food appears relative to the frame. Portions look smaller, and mass and calories come out biased low, with no error message. The export notebook measures the source aspect ratio from the real training files rather than assuming it.

## Privacy by construction

The app's manifest has no `INTERNET` permission, and inference requires no server. This does not rule out operating-system backups or explicit sharing through other apps. The current manifest enables backup with largely default rules. See the [storage plan](../planning/data-storage.md) for the proposed policy and remaining decisions.

## Honest UI

Two features in the original mockups described outputs this model doesn't have:

- a **per-ingredient carb split** ("about 38 g from rice"). v4 has one head over the whole dish, with no per-ingredient output.
- a **cooking-cue or hidden-sugar read**. v4 dropped the text and cooking heads ([13](13-negative-result-text-heads.md)).

Both now appear only in the sample-data state. A live estimate shows the model's measured test error instead, because that's information it actually has. The rule: the UI should never claim more than the model can back up.

## Known gaps and next steps

The table below preserves research-era directions. Current priorities, including the professor's reported glucose model, are tracked in the [roadmap](../planning/roadmap.md). Suggested fixes here require validation; they are not established guarantees.

| Gap | Why it matters | What would fix it |
|---|---|---|
| **No depth input** | Mass is the weakest target. Area in a photo doesn't tell you height. | Nutrition5k includes overhead depth. Add it as a second input channel and retrain. This needs a phone with a depth sensor, or a monocular depth model. |
| **Overhead framing is advisory** | The system camera can't enforce a top-down shot, and angled photos are out of distribution. | Use CameraX, check the phone's tilt, and only enable the shutter when it's close to flat. |
| **int8 accuracy** | int8 is 4x smaller and fastest on CPU, but calibration-only quantization may cost too many grams. | Quantization-aware training, which teaches the model to be robust to 8-bit rounding. |
| **Thermals are unmeasured** | B3 at 300 px is the heaviest model in the paper, and repeated scans may throttle the phone. | Measure sustained scanning on real devices. |
| **Cafeteria-only data** | Home lighting, plates and cooking differ from two cafeterias. | Collect and label a small home-meal test set to measure the real-world gap. |
| **Downstream health models** | Nutrition numbers alone don't manage glucose. | Future work in the paper: predict post-meal glucose response and HbA1c from logged meals and activity. |

## Glossary

| Term | Meaning here |
|---|---|
| MAE | Mean absolute error. The average size of the miss, in real units. |
| RMSE | Root mean squared error. Squares misses first, so big misses dominate. |
| z-score | (value - mean) / standard deviation. What the head predicts ([06](06-target-scaling.md)). |
| Huber loss | Squared error for small misses, linear for big ones ([08](08-loss-and-training.md)). |
| Transfer learning | Starting from weights trained on another task (ImageNet) ([07](07-model-choice.md)). |
| Fine-tuning | Letting some pretrained layers keep training on the new task ([09](09-fine-tuning.md)). |
| BatchNorm | A layer that normalizes activations with a mean and variance. Kept frozen here ([09](09-fine-tuning.md)). |
| GAP | Global average pooling. Averages each feature map to one number. |
| Augmentation | Label-preserving changes to training images ([10](10-resolution-and-augmentation.md)). |
| Data leakage | Information from validation or test reaching training ([05](05-split-and-leakage.md)). |
| Seed | The number that fixes all randomness in a run ([12](12-seeds-and-ensemble.md)). |
| Ensemble | Averaging several models' predictions ([12](12-seeds-and-ensemble.md)). |
| Pre-registration | Writing the decision rule before seeing the result ([11](11-validation-gate.md)). |
| Bootstrap CI | Uncertainty estimated by resampling the test predictions ([14](14-reading-the-results.md)). |
| LiteRT | The current name for TensorFlow Lite, the on-device runtime ([15](15-on-device-export.md)). |
| Quantization | Storing weights with fewer bits (float16, int8) to shrink the model ([15](15-on-device-export.md)). |
| Delegate | Hands the model's math to a GPU or NPU instead of the CPU. |
| Fixture test | Runs a pinned dish on the phone and checks it against the notebook's answer. |

## Check yourself

<details>
<summary>Why are these bugs called silent, and what makes them catchable?</summary>

None of them crash. Each produces reasonable-looking numbers, so nothing visibly breaks. They're catchable because the fixture test compares the phone's output for one known dish against the notebook's output for the same dish. Any of the three bugs makes those disagree.
</details>

<details>
<summary>What single addition would most improve the weakest target, and why?</summary>

Depth. Mass depends on volume, and an overhead RGB photo only shows area. Depth adds the height of the food, which is the missing dimension.
</details>
