# Product requirements

Status: proposed implementation specification, informed by the user directions in the [decision register](README.md#decision-register).

## Intended first experience

Photograph one meal from above, receive an accepted nutrition estimate, review or correct it, save it locally, and optionally obtain a glucose forecast when all required inputs are available.

Support Android and iOS. Core meal logging and inference should work without a server once models are installed. External glucose-data availability must be assessed separately.

## Required behavior

- Persist calories, mass, carbohydrates, protein, and fat in a single meal record.
- Support manual entry when capture or estimation is unavailable.
- Distinguish model estimates, user corrections, and measured glucose.
- Preserve history across process death, reboot, and application upgrades.
- Explain non-food, poor-photo, unsupported-scene, and uncertain outcomes.
- Never invent nutrition, confidence, glucose readings, ingredients, or personalized effects.
- Missing/broken models must produce an unavailable state, not sample predictions.
- A failed new scan must not show a previous scan's result as its own.
- Save a valid meal even if glucose prediction is unavailable.
- Editing inputs invalidates dependent forecasts; recompute explicitly.
- Allow record deletion, image removal, and a visible storage summary.
- Keep any demonstration mode clearly isolated from real history.

## Supported input proposal

Start with one visible meal from an overhead image. Evaluate mixed meals, drinks, soups, packaging, shared plates, screenshots, and unfamiliar cuisines before claiming support. Food presence is not sufficient evidence that the nutrition model can estimate it accurately.

Do not promise zero false positives. Define acceptable measured error and coverage with uncertainty on a representative held-out dataset.

## User states

| State | Behavior |
|---|---|
| Capture cancelled | Return without saving or changing an existing meal |
| Poor image | Explain actionable issue and offer retake/manual entry |
| Non-food | Say no food detected; do not produce nutrition |
| Unsupported or uncertain | Explain inability to estimate; offer retake/manual entry |
| Running | Prevent duplicate work and identify the active scan |
| Accepted estimate | Show all nutrients and allow correction before save |
| Model unavailable | Keep manual logging available |
| Saved | Show only after durable database success |
| Save failed | Preserve editable draft and offer retry |
| Forecast unavailable | Preserve meal; state missing/stale inputs or model failure |

## Scope boundaries

No insulin-dose recommendation, diagnosis, HbA1c estimate, per-ingredient attribution, or hidden-sugar detection is included in the first-version proposal. None is established by the current nutrition model. Glucose forecasting requires its own validation for the intended use and population.

Cloud accounts, automatic sync, depth capture, multi-view inference, and on-device retraining are deferred, not rejected permanently.

## Existing gaps

Current Android code has three UI experiences and separate in-memory histories. Fusion's manual-entry form collects extra nutrients that its save handler does not retain. Profile inputs do not affect nutrition inference. Glucose numbers are sample data. iOS has no implementation here.

Related: [data design](data-storage.md), [model flow](model-integration.md), [tests](validation.md).
