# ByteBite

Private source, research, and implementation planning for an Android and iOS app that estimates meal nutrition from an overhead photo and aims to integrate a separate glucose-prediction model.

**Current status: Android research prototype.** The nutrition inference code exists, but trained model assets are not committed. Meal history and profiles are held in memory. Glucose displays contain sample data. There is no implemented iOS app in this repository.

## Start here

| Need | Read |
|---|---|
| Decisions, proposals, and open questions | [Planning index](docs/planning/README.md) |
| First-version scope and user behavior | [Product requirements](docs/planning/product-requirements.md) |
| Local records, storage budgets, retention, and backup | [Data and storage](docs/planning/data-storage.md) |
| Food rejection, nutrition inference, and glucose integration | [Model integration](docs/planning/model-integration.md) |
| Test cases and release evidence | [Validation plan](docs/planning/validation.md) |
| Ordered implementation milestones | [Roadmap](docs/planning/roadmap.md) |
| Run the existing Android prototype | [Android instructions](android/README.md) |
| Research results and rationale | [Research index](docs/research/README.md) and [historical overview](docs/research/overview.md) |

## What exists and what remains

| Area | Existing implementation | Planned work |
|---|---|---|
| Capture | System camera and gallery, image downsampling and orientation correction | Quality and supported-scene checks; failure recovery |
| Nutrition | EfficientNetB3 inference through LiteRT; model metadata and export notebook | Recover authoritative weights, validate exports and real-world accuracy |
| Food recognition | No food/non-food gate | Evaluate a small classifier plus uncertainty and scene checks |
| Meal history | Separate in-memory lists for three experiences | One persistent record containing all nutrients and user corrections |
| Glucose | Demonstration UI | Integrate professor's model after inspecting its input/output contract |
| iOS | No app implementation | Native app and validated on-device model export |
| Tests | Unit tests and optional model fixture test | Full pipeline, storage, rejection, and device test coverage |

A missing model currently activates labelled sample data. This is demo behavior, not the planned production fallback. The model fixture test skips without its assets; a green run does not establish working inference.

The professor's glucose model is reported to exist, but its artifact, required inputs, mobile compatibility, and validation results have not been reviewed here.

## Repository layout

- `android/`: prototype, tests, and Android build instructions.
- `notebooks/`: model comparison, export notebook, and v4 walkthrough.
- `docs/planning/`: current implementation plan; **not implemented functionality**.
- `docs/research/`: preserved research results, explanations, and known inconsistencies.
- `paper/`: original paper and research figures.

The public repository is [Ismail-Abu/ByteBite](https://github.com/Ismail-Abu/ByteBite). This private repository retains its existing name, `ByteBite-archive`; it has not been renamed or synchronized to the public repository.

## Before implementation

Start with [milestone 0](docs/planning/roadmap.md#milestone-0-model-and-product-baseline). Locate the trained weights, reconcile the documented result/split differences, and obtain the glucose-model contract. Do not treat a research MAE as a per-photo confidence interval.

No production data, user photos, model weights, credentials, or patient datasets belong in Git.

## Research credit

NSSRP undergraduate research, Benedictine University. Authors: Saim Sultan, Ismail Abu-Shanab, and Husam Ghazaleh, Ph.D. Original methods, results, and references are preserved in the [research overview](docs/research/overview.md).
