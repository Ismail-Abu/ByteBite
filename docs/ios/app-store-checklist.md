# App Store and HealthKit Checklist

What it takes to ship ByteBite on the App Store, with a focus on the health-data
requirements.

## Entitlements and capabilities

- **Camera usage** — `NSCameraUsageDescription` with a clear, honest string
  ("ByteBite uses the camera to estimate the nutrition of your meal from a photo.").
- **HealthKit** — enable the HealthKit capability; add `NSHealthUpdateUsageDescription`
  (and `NSHealthShareUsageDescription` if reading back).
- **Photo library** (optional) — only if you allow estimating from an existing photo;
  request add/read scopes accordingly.

## HealthKit integration

- Request authorization to write **dietary carbohydrates** (`HKQuantityTypeIdentifier
  .dietaryCarbohydrates`) and **dietary energy** (`.dietaryEnergyConsumed`), plus protein
  and fat if desired.
- Write only on explicit user action ("Save to Health"), never silently.
- Use correct units: kcal for energy, grams for macros.

## App Review considerations

- **Health accuracy disclaimer.** State clearly that estimates are approximate and not a
  substitute for medical advice or clinical carb counting. Diabetes is a sensitive domain;
  reviewers will look for this.
- **Privacy nutrition label.** Declare that photos and estimates stay on device and are not
  collected. If that is true, say so plainly.
- **No misleading medical claims.** ByteBite *supports* self-management; it does not
  diagnose or dose insulin.

## Pre-submission testing

- TestFlight build validated on multiple device sizes.
- Numerical parity check passing (Core ML vs. reference model).
- Full offline run in airplane mode.
- Accessibility: VoiceOver reads the five nutrition values and the disclaimer.

## Not for this release

- Cloud sync, accounts, sharing. Keep v1 local-only to sidestep data-handling complexity
  and to make the privacy story simple and true.
