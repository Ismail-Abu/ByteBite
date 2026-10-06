# Implementation planning

Updated: 2026-10-05. These documents capture the planning discussion; they do not claim the features have been built or validated.

## Reading order

1. [Product requirements](product-requirements.md)
2. [Data and storage](data-storage.md)
3. [Model integration](model-integration.md)
4. [Validation](validation.md)
5. [Roadmap](roadmap.md)

## Decision register

| ID | Status | Direction |
|---|---|---|
| D01 | User direction | Plan before app implementation; record the plan in this private repository. |
| D02 | User direction | Target Android and iOS; prefer on-device inference and storage. |
| D03 | User direction | Keep logs compact and explicitly handle non-food photos and error cases. |
| D04 | User-provided fact; unverified | Professor has a working glucose model. Artifact and validation are not yet reviewed. |
| P01 | Proposed | Room on Android; SwiftData or Core Data on iOS according to minimum OS. |
| P02 | Proposed | Keep structured meal records until user deletion; optional thumbnails retained for 90 days by default. |
| P03 | Proposed | Start with a separate small food classifier; keep existing nutrition model as the baseline. |
| P04 | Proposed | Require review of an estimate before saving; never log rejected scans as meals. |
| P05 | Proposed | Android LiteRT and iOS Core ML; confirm conversion support against actual artifacts. |
| P06 | Proposed | First version supports one overhead meal per photo; expand only after evaluation. |

Proposed defaults are not final decisions. Update this table when a proposal is accepted, rejected, or superseded, recording the reason and date.

## Open questions

- Which trained weights, split, scaler, and results are the authoritative nutrition baseline?
- What are the glucose model's inputs, output horizon, target users, artifact format, license/permission, and held-out results?
- Where will current glucose readings and any other required inputs come from? Offline inference does not guarantee offline access to every data source.
- Which foods/scenes, minimum OS versions, and reference devices are supported?
- Is storage strictly local, or may users enable backup/export? What recovery behavior is expected?
- Which existing UI experience should become the main app?
- What measured rejection, prediction-error, latency, memory, and storage limits justify release?
- Should this repository eventually be renamed? No rename is part of this documentation update.

## Maintenance rules

Keep requirements here, research evidence in [research](../research/README.md), and runtime behavior in source. Link rather than duplicate detailed results. Add measured evidence and model versions to milestones as work completes. Do not label proposed thresholds or illustrative budgets as achieved results.

This cleanup preserves source, tests, notebooks, and the paper. It replaces the long root research README with navigation and moves that overview into the research folder. It does not delete useful experimental history or merge the public repository.
