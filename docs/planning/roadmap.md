# Implementation roadmap

All milestones are **not started**. This documentation update does not implement app features.

## Milestone 0: Model and product baseline

Inventory authoritative nutrition weights, preprocessing, scaler, split, and evaluation results. Resolve the paper/walkthrough mismatch. Obtain the professor's glucose-model contract and distribution permission. Select initial supported food scenes, user population, reference devices, minimum OS versions, and primary UI.

**Exit:** traceable model baseline; known glucose inputs; agreed scope and numerical evaluation targets. Storage/prototype work can proceed while glucose intake is pending.

## Milestone 1: Durable local meal logging

Implement one shared meal schema and repository, migrations, all five nutrients, source/corrections, manual entry, transaction-safe saves, and date handling. Add optional thumbnails and bounded cleanup. Decide backup/export policy before storing real user data.

**Exit:** persistence, correction, deletion, disk-full, restart, and migration tests pass. Measure database growth.

## Milestone 2: Reliable nutrition inference

Export the baseline, validate raw-image preprocessing parity, add explicit model/error states, remove production sample fallbacks, prevent stale results, and verify CPU/accelerated behavior.

**Exit:** real-device inference evidence and predeclared error/latency/memory limits met.

## Milestone 3: Food rejection and uncertainty

Collect representative food/non-food and unsupported-scene examples. Evaluate a small gate, quality checks, and rejection thresholds. Decide whether a calibrated uncertainty model is needed before displaying intervals.

**Exit:** held-out rejection, coverage, and accepted-photo accuracy meet agreed thresholds. A detector that rejects everything does not pass.

## Milestone 4: Glucose integration

Implement the professor's contract, valid input acquisition, freshness/unit checks, versioned forecasts, and invalidation on correction. Validate estimated-nutrient inputs against measured-nutrient inputs and actual glucose outcomes.

**Exit:** combined pipeline meets declared criteria; missing inputs leave meal logging functional.

## Milestone 5: iOS implementation and parity

Build native capture, persistence, and model services using the shared contracts. Convert and validate model artifacts. Match accepted/rejected/error behavior and migration/retention semantics. HealthKit is optional and separately scoped.

**Exit:** shared fixture and acceptance tests pass on reference iPhones. Initial conversion feasibility should be checked in milestone 0, not postponed until here.

## Milestone 6: Pilot and release readiness

Run representative user/device tests, validate supported scenes, finish accessibility and recovery, measure sustained inference and long-history storage, and finalize data controls and product claims.

**Exit:** evidence in the [validation matrix](validation.md), with remaining limitations stated. No automatic publication is implied by this plan.

## Deferred work

Depth, multi-view capture, alternate backbones, cloud sync/accounts, on-device learning, and further health predictions remain separate proposals. Preserve the research history while prioritizing the minimum complete, validated local experience.
