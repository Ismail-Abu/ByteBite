# Multi-View Fusion

The single-overhead-photo constraint is what makes mass estimation hard: one flat view hides
height. Users willing to take a second photo (e.g., a slight angle) could get a better
estimate. This is an opt-in accuracy mode, not a replacement for the one-photo flow.

## The idea

- **Primary:** overhead RGB (unchanged, always required).
- **Optional second view:** a low-angle or oblique shot that reveals food height/stacking.
- Fuse features from both views before the regression head.

## Architectures

- **Shared-weight siamese encoder** over both views, pooled features concatenated. Reuses
  one backbone, keeps the model small.
- **Attention pooling** across an arbitrary number of views, so 1 or 2 photos both work with
  the same weights (train with view dropout so single-view still performs).

## Training data caveat

Nutrition5k provides fixed overhead (and side) captures, not free-form user angles.
Options:

- Use the dataset's existing side-angle captures where available to prototype fusion.
- Simulate second views via mild 3D-style warps or by rendering, acknowledging the domain
  gap to real user photos.

## Product tradeoff

Two photos add friction, and friction is the exact problem ByteBite exists to remove. So:

- Keep one-photo as the default and always-supported path.
- Offer "add a side photo for a better estimate" only when uncertainty is high (ties into
  `uncertainty-calibration.md`).
- Measure whether the second view actually reduces mass MAE enough to justify the extra tap.
