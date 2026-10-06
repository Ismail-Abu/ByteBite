# Data and storage

Status: proposed design. Database schema and retention policy are not implemented.

## Storage architecture

Use an application-private local database: Room on Android; SwiftData or Core Data on iOS after selecting the minimum OS. Keep images as private files referenced by records, not base64 strings inside rows. Use OS data protection and decide whether additional database encryption is needed; keep any encryption keys in platform key storage, never Git.

Use the same logical schema and unit definitions on both platforms. Implement versioned migrations without destructive fallback.

## Logical records

| Record | Minimum content |
|---|---|
| Meal | Stable UUID, occurrence time in UTC plus original zone/offset, creation/update time, optional name, current revision |
| Meal revision | Five nutrient values with explicit units; origin: inferred/manual/corrected; original estimate reference; correction timestamp |
| Nutrition estimate | Scan ID, raw five outputs, validated values, acceptance status/reason, model and preprocessing versions, measured inference duration |
| Photo | Meal/scan reference, private relative path, byte size, capture time, expiry; optional |
| Glucose prediction | Meal revision, model version, input snapshot/references, prediction creation time, forecast timestamps/values/units, validity status |
| Glucose observation | Actual measured value, unit, timestamp, source and source ID for deduplication |
| Model manifest | Version, artifact hash, schema/preprocessing version, units, input/output contract; shared across records |
| Profile | Only fields required by the selected product/model; version relevant inputs |

Missing values are null, not zero. Reject nonfinite numbers. Store sufficient numeric precision; round only for display. Use explicit glucose-unit conversion. Preserve original estimates separately from corrections, and never count both as separate meals.

Prediction input snapshots must preserve reproducibility when meal/profile values change. If the professor's model requires long time windows, store shared referenced observations rather than duplicating entire histories per prediction. Final glucose schema depends on its contract.

## Retention proposal and budget

- Keep structured records until user deletion.
- Keep optional compressed thumbnails for 90 days by default; offer longer retention or no images.
- Treat full-resolution captures as temporary, with cleanup after processing, cancellation, and recovery from interruption.
- Never delete the user's original gallery photo.
- Cap diagnostic logs and temporary files; exclude photos and detailed health values from routine logs.
- Keep model artifacts once per active version, not per meal. Bound rollback artifacts.
- Define observation retention only after the glucose model's required history is known.

Illustrative budget at five meals/day, using decimal KB/MB:

| Assumption | One year |
|---|---:|
| 2 KB structured data per meal | 3.65 MB |
| 50 KB thumbnail per meal | 91.25 MB |
| 90-day thumbnail window | 22.5 MB retained at a time |

These are assumptions, not benchmarks. They exclude database pages/indexes/journals, prediction curves, glucose observations, app binaries, and models. For a glucose stream, budget sample count × stored bytes per sample plus database overhead. Measure a representative populated database before setting a total size limit.

The research documents report approximately 22.3 MB for the nutrition float16 artifact. Food-gate and glucose-model sizes remain unknown. Installed storage and peak inference RAM are different budgets and must both be measured.

## Save, edit, and delete

Save related structured records in a transaction; use stable IDs and idempotent retries to prevent duplicates. Stage thumbnails and clean orphan files after interrupted operations. A missing thumbnail must not make a meal unreadable.

On nutrition/profile/input changes, invalidate affected forecasts and create a new prediction revision when recomputed. On deletion, remove associated private photos and dependent prediction/input data according to the defined relationship; retain no hidden duplicate health history.

Use paged history queries and date indexes. Compute summaries from stored records rather than keeping screenshot charts. Test multi-year history and migrations.

## Privacy, backup, and recovery

No INTERNET permission does not prevent OS-managed backup. On Android this is now configured explicitly: the meal-history database and app-owned meal images are excluded from both cloud backup and device-to-device transfer (`android/app/src/main/res/xml/data_extraction_rules.xml` and `backup_rules.xml`), while non-health settings (the theme choice) remain eligible. Health data therefore stays on the device; the tradeoff is that OS transfer does not carry history to a new device. iOS must be configured consistently when implemented.

Strict local-only storage means loss/uninstall can lose history. Decide on optional user-initiated encrypted export/import and document that tradeoff. Automatic cloud synchronization is not part of this plan. Research uploads or telemetry require a separate, explicit design and consent.

## References

- [Android local storage](https://developer.android.com/training/data-storage/)
- [Apple persistent model data](https://developer.apple.com/documentation/swiftdata/preserving-your-apps-model-data-across-launches)
- [Android backup behavior](https://developer.android.com/identity/data/autobackup)
