# Food-Aware Data Augmentation

With only ~3,259 dishes after cleaning, the models are data-limited. Better augmentation is
the cheapest way to improve generalization and shrink the train/validation gap.

## Augmentations that respect the physics

The label is nutrition for the food in the frame, so augmentations must not change how much
food is implied:

- **Safe (geometry-preserving):** horizontal/vertical flips, small rotations (overhead
  shots have no canonical up), slight translation. Free and label-preserving.
- **Photometric:** brightness, contrast, hue/saturation jitter, and simulated white-balance
  shifts. Cafeteria lighting varies; this is the real-world variance to cover.
- **Careful:** random crop / zoom **changes the apparent portion** and therefore the
  implied mass. Use only mild crops, or not at all, unless labels are scaled accordingly.
- **Avoid:** heavy Cutout/erasing over the plate (removes food the label still counts) and
  aggressive zoom.

## Regularization-style augmentation

- **MixUp / CutMix on regression targets:** blend two dishes and blend their nutrition
  labels by the same weight. Works for regression and adds useful interpolation. Validate —
  it can blur sharp portion boundaries.

## Test-time augmentation (TTA)

Average predictions over a few flips/rotations at inference. Small, reliable accuracy bump
for a latency cost — reasonable on the Neural Engine, optional on weaker devices.

## What to measure

- Train vs. validation MAE gap before and after (overfitting indicator).
- Per-nutrient test MAE with CIs.
- Robustness slice: evaluate on artificially dimmed/blurred test images to confirm
  photometric augmentation actually helped real-world robustness.
