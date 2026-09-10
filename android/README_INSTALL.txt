ByteBite GUI prototype - install into your existing "Guione" Android Studio project
===================================================================================

WHAT THIS IS
Native Compose recreation of the two HTML mockups:
- Kitchen Journal (Snap / Plate / Week + calendar)
- Glucose (Camera start / Today / Scan result / Log + calendar)
One app, chooser screen first. UI only, all data hardcoded in SampleData.kt.

INSTALL (5 steps)
1. Close Android Studio (or at least the Guione project).
2. Extract this zip DIRECTLY INTO the Guione project folder
   (the folder that contains "app", "gradle", "build.gradle.kts").
   Say YES to overwrite when asked. It overwrites exactly 5 files:
     gradle/wrapper/gradle-wrapper.properties
     gradle/libs.versions.toml
     build.gradle.kts
     app/build.gradle.kts
     app/src/main/java/com/example/guione/MainActivity.kt
   and adds 4 new Kotlin files next to MainActivity.
3. Reopen the project. When prompted, "Sync Now" (or File > Sync Project with Gradle Files).
   First sync downloads Gradle 8.14.2 + Compose libraries - needs internet, takes a few minutes.
4. Run the "app" configuration on your Medium Phone emulator.
5. Chooser screen appears: pick Kitchen or Glucose. System Back returns to the chooser.

NOTES
- Do NOT delete the old template files (FirstFragment, SecondFragment, layouts, menus).
  They are kept compiling on purpose; they are just unused.
- Fonts: Fraunces/Nunito/Sora/Manrope are stand-ins for now (system serif + sans).
  To use the real ones later: download the four .ttf files, put them in app/src/main/res/font/,
  and swap the FontFamily vals at the top of KitchenUi.kt / GlucoseUi.kt.
- Everything editable lives in SampleData.kt (meals, macros, glucose numbers, swaps).
