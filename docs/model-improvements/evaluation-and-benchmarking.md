# Evaluation and Benchmarking

The harness that keeps every improvement honest. If evaluation is sloppy, "improvements" are
just noise.

## Fixed protocol

- **Split:** the paper's dish-level 70/15/15 (2,281 / 488 / 490), built on fixed indices,
  verified no dish appears in more than one set. Never re-split per experiment.
- **Seed:** the same seed for all models so comparisons are apples-to-apples.
- **Metric:** mean absolute error per nutrient (calories, mass, carbs, protein, fat).
- **Uncertainty on the metric:** 95% bootstrap confidence intervals over 1,000 resamples of
  pooled test predictions from multiple runs. A change without separated CIs is not a result.

## Headline metric

Carbohydrate MAE is the primary number — it drives insulin dosing and is the clinical point
of the project. Report it first; a model that improves calories but worsens carbs is a
regression.

## Baselines to always include

- SnapNutrition (the public baseline from the paper).
- Current shipped EfficientNetB3.
- A trivial predict-the-training-mean baseline, so absolute skill is visible.

## Slices worth tracking

- By dish complexity (single-item vs. mixed plate).
- By calorie range (does it fail on very large or very small portions?).
- Robustness: dimmed, blurred, and off-angle versions of the test set.

## Reproducibility

- One command runs the full eval and prints the MAE-with-CI table.
- Log model hash, data split hash, and seed with every result.
- **On-device parity:** the shipped TFLite/Core ML model must reproduce the reference MAE
  within 1% on the test set, checked automatically before release.
