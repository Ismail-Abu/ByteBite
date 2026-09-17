# ByteBite Android app

A native Kotlin / Jetpack Compose app that runs the ByteBite nutrition model on the phone. One app with three experiences, picked from a chooser screen:

- **Fusion**: scan, glucose, log, and manual entry with a profile. The combined experience.
- **Kitchen Journal**: snap, plate, and week views with a calendar.
- **Glucose**: camera start, today, scan result, and log with a calendar.

A scan in any experience is logged into that experience's history, and the Today carbohydrate totals follow it.

## Build and run

Requirements: Android Studio (or JDK 21 with the Android SDK), a device or emulator on Android 8.0 (API 26) or later.

1. Open this `android/` folder in Android Studio and let Gradle sync. The first sync downloads Gradle 8.14.2, Compose and the LiteRT runtime, so it needs internet and takes a few minutes.
2. Run the `app` configuration on a device or emulator.

From the command line:

```
./gradlew assembleDebug        # build the APK
./gradlew installDebug         # install on a connected device
./gradlew testDebugUnitTest    # model contract tests
```

## The model

The shutter takes a real photo and the five estimates (calories, mass, carbohydrate, protein, fat) come from the EfficientNetB3 model running locally through LiteRT. Nothing leaves the device: the app has no `INTERNET` permission.

The model file is not committed. Generate it with [`notebooks/bytebite_android_export.ipynb`](../notebooks/bytebite_android_export.ipynb), which writes the `.tflite`, its preprocessing contract and a parity fixture into `app/src/main/assets/`. See [`README_MODEL.md`](README_MODEL.md) for the conversion routes considered and the preprocessing details that have to match, and [`app/src/main/assets/README.md`](app/src/main/assets/README.md) for the generated files.

Without a model in `assets/`, the app still builds and runs as the UI prototype on the sample dish in `SampleData.kt`, and says so on screen.

## Notes

- Logs live in memory for the session. Closing the app resets them to the sample history, which keeps back-to-back demos consistent.
- No `CAMERA` permission is needed: capture goes through the system camera app via the `FileProvider` declared in the manifest.
- The instrumented test `ModelFixtureTest` checks that on-device output matches the notebook on a fixture dish: `./gradlew connectedDebugAndroidTest` with a device attached and the model exported.
