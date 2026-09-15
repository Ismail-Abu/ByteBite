# Generated model assets

Everything else in this directory is written by
`notebooks/bytebite_android_export.ipynb` and is **not** committed — only this
README is tracked.

| file | produced by | purpose |
|---|---|---|
| `bytebite_<model>.tflite` | export notebook, Cell 8 | the gated model variant, named after the `.keras` it came from |
| `bytebite_model.json` | export notebook, Cell 8 | input geometry/range and the train-only `mu`/`sd` |
| `fixture_dish.png` | export notebook, Cell 8 | one Nutrition5k test dish, pre-resized to 300×300 |
| `fixture_dish.json` | export notebook, Cell 8 | that dish's expected output, for `ModelFixtureTest` |

The app builds and runs with this directory empty: `NutritionEstimator.loadOrNull`
returns null, `ScanStore` reports `NoModel`, and both experiences fall back to the
hardcoded dish in `SampleData.kt` while labelling it as sample data. So a fresh
clone is still the working UI prototype it was before the model existed.

To install the model, run the export notebook on the machine that holds the
trained weights, with `BYTEBITE_ANDROID_ASSETS` pointing here:

```
NUTRITION5K_DIR=/path/to/nutrition5k_dataset \
BYTEBITE_V4_MODEL=outputs/v4_s42.keras \
BYTEBITE_SPLIT_CSV=outputs/data_split_seed42.csv \
BYTEBITE_ANDROID_ASSETS=android/app/src/main/assets \
jupyter nbconvert --to notebook --execute notebooks/bytebite_android_export.ipynb
```

Then confirm the phone agrees with the notebook:

```
cd android && ./gradlew connectedDebugAndroidTest
```
