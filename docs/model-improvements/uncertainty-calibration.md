# Uncertainty and Calibration

A health tool should say how sure it is. A confident wrong carb estimate is worse than an
honest "I'm not sure about this one." Right now ByteBite returns point estimates with no
confidence.

## Why it matters here

Carbohydrate estimates inform insulin decisions. If the model is shaky on a stacked or
unusual plate, the app should surface that and nudge the user to double-check, rather than
present a precise-looking number.

## Ways to get uncertainty

- **Heteroscedastic regression.** Predict both a mean and a variance per nutrient (Gaussian
  negative-log-likelihood loss). Gives a per-photo, per-nutrient confidence for free at
  inference. Cheapest and most deployable.
- **MC Dropout.** Keep dropout active at inference and sample several forward passes; use the
  spread as uncertainty. Multiplies inference cost, but no retraining.
- **Deep ensembles.** Variance across the existing models is a strong uncertainty signal.
  Accurate but expensive on-device; pairs well with distillation.

## Calibration

Raw variances are usually mis-scaled. After training:

- Plot predicted vs. actual error and fit a simple temperature/scale correction on the
  validation set.
- Report a calibration metric (e.g., expected calibration error for regression intervals):
  do 90% intervals actually contain the truth 90% of the time?

## Product surface

- Show a band, not just a number: "Carbs: 45 g (±8)".
- If uncertainty exceeds a threshold, prompt: "This plate is hard to read — try a clearer
  overhead photo."
- Never auto-write a low-confidence estimate to HealthKit without confirmation.
