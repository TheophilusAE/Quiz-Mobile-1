# Quizapp - Task list with Firestore (Compose)

This branch adds a simple Jetpack Compose UI and Firestore-backed repository for the quiz assignment: a list of tasks with title, date/time and done flag.

What I changed

- Added a `Task` data model and a `FirestoreRepository` that observes the `tasks` collection.
- Added `TaskViewModel` exposing `tasks` as a `StateFlow` and methods to add/toggle tasks.
- Added `TaskListScreen` Compose UI with an Add dialog, list, and simple toggle behavior.
- Updated `app/build.gradle.kts` to include Firebase BOM + Firestore and coroutines dependency.

Important: Firebase configuration required

1. Create a Firebase project at https://console.firebase.google.com/ and register an Android app with package name `com.example.myapplication` (or change the package in `AndroidManifest.xml` to match your Firebase app).e: file:///D:/Quizapp/app/src/main/java/com/example/myapplication/ui/TaskListScreen.kt:33:37 Unresolved reference 'compose'.
Ask Gemini

2. Download the generated `google-services.json` and place it into `app/` (i.e. `d:\Quizapp\app\google-services.json`).
3. In Android Studio, add the Google Services plugin. Two common options:

   - Option A (recommended): In `app/build.gradle.kts` add the plugin and the classpath in the top-level build script if needed. Example (Groovy shown for reference):

     // In project-level build.gradle.kts add classpath for com.google.gms:google-services
     // In app module's build.gradle.kts add: plugins { id("com.google.gms.google-services") }

   - Option B: Initialize Firebase manually in code using `FirebaseOptions` constructed from the `google-services.json` contents. (More advanced.)

4. Sync Gradle and run the app. The app will listen to a Firestore collection named `tasks`. When you add tasks in the app they will be uploaded to Firestore and shown on other devices connected to the same collection.

Notes and limitations

- This implementation expects Firestore rules to allow the app to read/write `tasks` for development. Configure security rules for production use.
- I intentionally kept the UI minimal: the add dialog uses the platform DatePicker/TimePicker; formatting is basic.
- If you prefer running locally without Firebase setup, you can switch the repository to a simple in-memory store (easy to do).

Next steps I can help with

- Add automatic Firebase initialization or add the `google-services` plugin to Gradle files.
- Improve the UI (Checkbox, better cards, swipe-to-delete).
- Add tests for `TaskViewModel`.
