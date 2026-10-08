# Alice

A habit and change tracker for Android, built for the IU course *Mobile Software Engineering II* (DLBCSEMSE02), Task 1: tracking app.

Alice helps you become a better you one small step at a time. Every week you add one new step:

- **Habits**, something you do (read 10 pages, stretch),
- **Swaps**, something you use instead (a loofah instead of a sponge),
- **Avoids**, a company, food or product you stay away from.

A new step is ticked every day for 66 days, the median time to form a habit found by Lally et al. (2010). After that it moves to a 3-Month Check-Up that everyone does on the same days (1 January, April, July and October), and each check-up ends with a shareable summary.

## Features

- Habits grid with anytime and end of day steps, locked until the daily check-in time
- Repeating steps: specific days with counts, or a number of times per day, week, month or every X days
- One-off steps that move to Changes, Finished, once done
- Daily Check-In (stacked missed days, skip, slip reasons), Weekly Check-In (pick next week's step from 300 ideas), 3-Month Check-Up with the refresher rule
- One habit screen with streaks, success rate and week, month, year and all-time charts
- Edit any step with the same form it was made with, a new schedule applies from that day and the past keeps its ticks
- Changes, Avoid list (search, filters, verdicts) and Avoid item (why, brands, alternatives)
- Insights per week, month, year and all time, with slip reasons and past summaries
- Nine 3-Month summary styles with a QR code, shared as a picture through the Android share sheet or saved to the gallery
- Ten colour themes, light and dark
- Tutorial, daily nudge notification, export and import of all data as JSON
- Pronouns in Settings (she/her, he/him, they/them) so gendered languages address you the right way, neutral wording by default
- 56 languages with a language picker in Settings (English, Slovak, Czech and Russian checked by the author, the rest waiting for native speakers), right to left for Arabic, Hebrew, Persian and Urdu, and a note asking for corrections by email

## Building

Open the folder in Android Studio and press Run, or from a terminal:

```
./gradlew assembleDebug              # build the app
./gradlew testDebugUnitTest          # run the unit tests (JVM)
./gradlew connectedDebugAndroidTest  # run the device tests (database, migration, tabs) on a phone or emulator
```

Release builds are signed with a key kept outside the repository in `../Keystore/` (`keystore.properties` plus the `.jks` file); without it `assembleRelease` gives an unsigned APK.

The app is portrait only. All text the user sees is in `res/values/strings.xml`, translated in the `values-*` folders and listed for Android 13+ in `res/xml/locales_config.xml`. The 300 step ideas are in `assets/ideas.tsv`, translated per language as `assets/ideas-<language>.tsv`. Room writes each database version's layout to `app/schemas`, which the migration test reads.

Debug builds include `DemoDataActivity`, which fills the app with sample data:

```
adb shell am start -n com.lucyprockova.alice/.DemoDataActivity
```

In debug builds every other screen can be opened from adb too. A main tab opens with an extra:

```
adb shell am start -n com.lucyprockova.alice/.ui.MainActivity --es tab insights
```

## Structure

| Package | What it holds |
|---|---|
| `data` | Room entities, DAO and database, settings, the repository, backup (JSON) and the step ideas |
| `logic` | Pure Kotlin rules with no Android code: phases and repeats (`StepRules`), grid squares (`Grid`), due check-ins (`CheckIns`), statistics (`Stats`) |
| `ui` | Jetpack Compose with Material 3. `MainActivity` hosts the four main tabs (Habits, Changes, Avoid, Insights) behind one navigation bar, each tab keeping its own state; every other screen is its own activity, one file per screen |
| `ui.summary` | The nine 3-Month summary styles, one file each, plus the parts they share |
| `notify` | The daily nudge alarm and notification |

Every screen observes one `Snapshot` of the data, built from Room flows, so a change anywhere updates every screen.

Kotlin, Jetpack Compose, Material 3, Room, JUnit 4, AndroidX Test and Compose UI tests, ZXing (QR codes). Minimum Android 8.0. Release builds are shrunk with R8.
