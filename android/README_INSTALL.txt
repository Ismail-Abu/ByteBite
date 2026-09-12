ByteBite Android app - install into your existing "Guione" Android Studio project
=================================================================================

WHAT THIS IS
Native Compose recreation of the two HTML mockups, now running the real model
on the device:
- Kitchen Journal (Snap / Plate / Week + calendar)
- Glucose (Camera start / Today / Scan result / Log + calendar)
One app, chooser screen first.

The shutter takes a real photo and the five nutrition numbers come from the
v4 EfficientNetB3 model running locally through LiteRT. With no model installed
in assets/ the app falls back to the hardcoded dish in SampleData.kt and says so
on screen, so it still works as the UI prototype.

See README_MODEL.md for how the model gets converted and installed, and
app/src/main/assets/README.md for the generated asset files.

INSTALL
1. Close Android Studio (or at least the Guione project).
2. Extract this directly INTO the Guione project folder (the one containing
   "app", "gradle", "build.gradle.kts"). Say YES to overwrite.

   Overwrites:
     gradle/wrapper/gradle-wrapper.properties
     gradle/libs.versions.toml                  (+ tflite, exifinterface)
     build.gradle.kts
     app/build.gradle.kts                       (+ deps, noCompress "tflite")
     app/src/main/AndroidManifest.xml           (+ FileProvider)
     app/src/main/java/com/example/guione/MainActivity.kt
     app/src/main/java/com/example/guione/KitchenUi.kt
     app/src/main/java/com/example/guione/GlucoseUi.kt

   Adds:
     app/src/main/java/com/example/guione/CalendarUi.kt
     app/src/main/java/com/example/guione/SampleData.kt
     app/src/main/java/com/example/guione/NutritionEstimator.kt   on-device inference
     app/src/main/java/com/example/guione/ScanStore.kt            scan state
     app/src/main/java/com/example/guione/DishCapture.kt          camera + gallery
     app/src/main/res/xml/file_paths.xml
     app/src/test/java/com/example/guione/ModelSpecTest.kt
     app/src/androidTest/java/com/example/guione/ModelFixtureTest.kt

3. Reopen the project and "Sync Now" (or File > Sync Project with Gradle Files).
   First sync downloads Gradle 8.14.2, Compose, and the LiteRT runtime - needs
   internet, takes a few minutes.
4. Run the "app" configuration on a device or the Medium Phone emulator.
5. Chooser screen appears: pick Kitchen or Glucose. System Back returns to the
   chooser.

NOTES
- If your project already has newer SampleData.kt / MainActivity.kt than this
  bundle (for example a third "Fusion" experience), keep yours and take only the
  three new Kotlin files plus the KitchenUi/GlucoseUi, gradle, and manifest
  changes. This bundle's copies are the two-experience version.
- No CAMERA permission is needed: capture goes through the system camera app.
  The FileProvider entry in the manifest is required for that, though.
- Do NOT delete the old template files (FirstFragment, SecondFragment, layouts,
  menus). They are kept compiling on purpose; they are just unused.
- Fonts: Fraunces/Nunito/Sora/Manrope are stand-ins for now (system serif + sans).
  To use the real ones later: download the four .ttf files, put them in
  app/src/main/res/font/, and swap the FontFamily vals at the top of
  KitchenUi.kt / GlucoseUi.kt.
- Sample values (used when no model is installed) live in SampleData.kt.
