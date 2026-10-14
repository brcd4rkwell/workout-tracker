# Workout Tracker

A fully offline Android app for running your own workout program: build your
weeks/days/exercises, log weight/reps/sets as you train, get an auto-starting
rest timer and a warm-up timer, and keep a permanent history of every workout
for later analysis — all stored locally on your phone (no internet, no
account, no cloud).

## Opening the project

1. Install **Android Studio** (the current stable release — "Ladybug" or
   newer works fine): https://developer.android.com/studio
2. Unzip this project anywhere on your computer.
3. In Android Studio: **File → Open**, and select the unzipped `WorkoutApp`
   folder (the one containing `settings.gradle.kts`).
4. Let Android Studio sync Gradle the first time — it will download the
   Gradle distribution, the Android SDK platform, and all the libraries this
   project uses. This needs an internet connection *once*; the built app
   itself never uses the internet.
5. Plug in an Android phone (with USB debugging enabled) or start an
   emulator, then press the green ▶ **Run** button.

If Gradle sync reports an error the first time, it's almost always Android
Studio asking you to install a missing SDK component (it will offer a
one-click "Install missing SDK package(s)" link) — accept it and sync again.

## How the app is organized

- **My Program** (home screen) — your training weeks, each as its own row,
  in the order you added them.
- Tap a week → its training **days** (e.g. "Day 1 - Push").
- Tap a day → the exercises planned for that day: sets, rep range, rest
  time, and your own note for each one. Edit, add, or delete exercises here
  any time — your plan is fully yours to change.
- **Start Workout** from a day screen walks you step by step: warm-up timer
  first, then each exercise in order, with a weight/reps entry for every
  set. Logging a set auto-starts the rest timer; tap **Next Exercise** when
  you're done with one.
- **History** (top-right icon on the home screen) lists every completed
  workout with its date and duration, and tapping one shows every set you
  logged that day, grouped by exercise — this is what's kept for your
  long-term progress tracking. Editing or deleting something from your plan
  later never touches this history.

## Notes on the data

Everything lives in a local SQLite database on the device (via Room) at
`workout_tracker.db`. There is no server, no sync, and no analytics —
deleting the app deletes the data, so back up your phone normally if you
want to keep your workout history long-term.

## A note on content

This app ships with **no pre-filled workout program** — you build your own
program using the in-app editor (Add Week → Add Day → Add Exercise). This
was a deliberate choice so the app works for any program you want to run,
typed in by you.
