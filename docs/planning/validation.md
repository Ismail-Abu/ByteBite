# Validation and acceptance plan

Status: planned tests, not completed evidence. Numeric release thresholds remain open.

## Dataset and measurement rules

Split by dish/person/source as appropriate to prevent related examples leaking into train, validation, and test. Include real phone photos across devices, lighting, portions, and cuisines. Keep held-out subjects for glucose evaluation and separate real-world data from the cafeteria benchmark.

Choose thresholds and model variants on validation data; reserve held-out test data for final reporting. Audit the export notebook's existing test-set quantization gate before adopting it as a release-selection protocol.

Measure non-food acceptance (accepted non-food / all non-food), food rejection, supported-meal coverage, and nutrition MAE on accepted photos. Report sample counts, subgroup results, and confidence intervals. Zero observed mistakes is not proof of zero real-world mistakes. Do not improve error metrics merely by rejecting nearly all meals.

Report per-nutrient error, large-error tails, and interval coverage when intervals exist. For glucose, use metrics and horizons appropriate to the professor's model and compare against relevant simple baselines. Test the full photo-to-glucose chain against measured outcomes.

## Acceptance matrix

| ID | Cases | Required behavior/evidence |
|---|---|---|
| IMG01 | Face, pet, furniture, empty plate, toy food | Rejection performance measured; rejected images yield no nutrients |
| IMG02 | Screenshots, printed images, food on a monitor | Supported-input policy evaluated, not assumed |
| IMG03 | Drinks, soups, packaged food, shared plates, unfamiliar cuisine | Per-category support/rejection and accuracy documented |
| IMG04 | Blur, darkness, glare, cropped plate, tiny food region | Actionable quality/scene failure or validated acceptance |
| IMG05 | EXIF rotations/mirroring, corrupt/large files | Correct geometry or explicit failure; bounded memory |
| NUT01 | Weighed meals and varying portion sizes | Per-nutrient error and large misses reported |
| NUT02 | NaN, infinity, invalid ranges, swapped output order | Reject invalid results; no fabricated zero values |
| MOD01 | Missing/corrupt weights, mismatched scaler/schema | Explicit unavailable state; release validation fails |
| MOD02 | Raw fixture through Python, Android, iOS | Agreement within predeclared tolerances for full preprocessing |
| MOD03 | GPU failure, CPU fallback, repeated scans | Valid result or explicit failure; latency/RAM/thermal measurements |
| FLOW01 | Cancel, double tap, new scan fails after success | No duplicate work/log; no stale result assigned to new scan |
| GLU01 | Missing/stale inputs, gaps, wrong units | No forecast from invented/default inputs; meal still saves |
| GLU02 | Meal/profile correction after forecast | Old forecast invalidated; replacement linked to exact revision |
| GLU03 | Predicted vs measured nutrients as inputs | End-to-end degradation and supported population documented |
| DB01 | Force-close/reboot during save, repeated retry | Atomic structured save; no duplicate meal or false success |
| DB02 | Full disk, thumbnail failure, orphan temporary file | Recoverable draft; old history intact; cleanup works |
| DB03 | Upgrade/migration with years of history | No data loss; queries remain responsive |
| DB04 | Thumbnail expiry and record deletion | Expiry retains numbers; deletion removes dependent private data |
| TIME01 | Midnight, DST, travel, manual time changes | Correct meal dates and forecast freshness |
| PRIV01 | Backup/export/restore and deletion | Matches chosen policy; no health data in routine diagnostics |
| UX01 | Offline use, accessibility, manual entry | Usable core flow; every entered nutrient preserved |

## Release evidence required

- Exact model artifacts/hashes, preprocessing versions, evaluation split IDs, and reproducible results.
- Signed-off supported-input policy and numeric acceptance thresholds; “TBD” is not a pass.
- Full inference tests with actual assets on both platforms; current skipped fixture tests are insufficient.
- Device matrix with cold/warm latency, peak memory, installed size, sustained scans, and offline behavior.
- Database size measured with representative meals, photos, and glucose history over multiple years.
- Migration, recovery, deletion, and backup-policy checks.
- Comparison of combined glucose predictions with measured outcomes in the intended population.
- Explicit separation between implemented features, remaining limitations, and clinical claims.

Record results next to each test ID as implementation proceeds. Do not treat this checklist as evidence that any test has run.
