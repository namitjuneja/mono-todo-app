# MONO

An Android to-do app that cuts list overwhelm by putting one task in front of you at a time.

## Download

[apk/MonoFocus-debug.apk](apk/MonoFocus-debug.apk) – open the file page on GitHub and tap Download, then install it on your phone.

## Features

### Tasks
- **Add** – type in the bar, one task per line. The + appears only when there is text.
- **Complete** – check it off; it stays in place, dimmed.
- **Steps** – a task's steps show under it in every list (and in the list widget). Tap a step's box to tick it; tap anywhere else on it to open the task's editor, just like the task itself.
- **Reorder** – hold a task anywhere and drag it up or down. There are no drag handles.
- **Edit** – tap a task for title, notes, steps and timer count. Native copy and paste work. Enter saves and closes.
- **Focus** – double-tap a task to start focus on it.
- **Insert where you choose** – tap the empty space just above or below any to-do or separator (including above the first and below the last). A thin inset line fades in there (yellow on the dark lists, near-black on the Day list) and the rows ease apart a little. Everything you add from the bar now goes into that spot: a to-do, several lines at once (in order, fading in one after another), or a separator via the dash button (the typed text becomes its label). When it is in, the line lets go and the keyboard hides. Tap the line again to deselect it; it also lets go by itself after 12 seconds of nothing happening (typing and adding keep it alive), when you switch lists, or when you open a task. With no spot chosen, adding still appends to the bottom. Taps near a row's edge choose the spot instead of opening that row; tap the middle of a row to open it.
- **Add bar** – one task per line. Markdown is understood: bullets (`-`, `*`, `+`, `1.`), checklists (`- [ ]`, `- [x]`), and `**bold**`, `~~strike~~` and backticks are stripped. A ticked box or a ~~crossed out~~ item arrives already done.
- **Archive** – swipe left on a task, or tap the archive icon in the top bar to archive the whole list. Neither asks first: an Undo button (the same one as on the focus screen) floats above the add bar for 5 seconds and puts everything back exactly where it was, separators included. Archiving the whole list removes its separators until you undo.
- **Nothing archives itself.**

### Separators
- **Add** – the dash button in the add bar; anything typed becomes the label.
- **Move** – hold it anywhere and drag; it lifts into a card with room above and below.
- **Rename or remove** – tap it (Enter saves and closes), or swipe left to delete.

### Focus
- **One task** – tap a task to see only it.
- **Next and previous** – swipe up or down, or tap the arrow.
- **Separators** – on Morning and Night, finishing the last task before a separator carries on into the next section, with a quiet rule and the separator's title (just the rule if it has none) in the middle of the top bar, below any camera cutout. Elsewhere, "Stop at separators" decides.
- **Scroll first** – when the next task is partly hidden, the list scrolls it to the middle before it opens.
- **Steps** – show every step, or just the next one.
- **Stuck menu** – try 2 minutes, break it into steps, or do something else first.
- **Done** – a wide pill (bigger in landscape); a short win moment, then the next task opens.
- **Double-tap** – double-tap empty space (including beside the list-button tab) to start focus on the first open task, with the same grow-into-focus animation as tapping it.
- **Landscape** – rotate for a flash-card view: the title is centred and large, steps are hidden.
- **Undo** – floats 10% from the top for 3 seconds after finishing. Leaving focus or moving to another task dismisses it.
- **Cleaning focus** – no stuck (?) button. Steps can be added in the editor only, not in focus.

### Timer
- **Meditation timer** – tasks that mention meditate, focus, mindful, breathe and the like get a quiet "5 min" capsule on their focus screen (length set in Settings). While running it shows the countdown with +1m, +5m and cancel; when it ends the capsule settles to a quiet colour and a soft bell rings three times. On day pages it is a moderately dulled black at rest and solid black while running; on dark pages it is white and grey, with the accent while running. It keeps running if you leave the screen.
- **2-minute timer** – start from the focus view.
- **Badges** – one per finished timer, on the task and in the list.
- **Editable** – change the count in the editor.

### Lists
- **Main, Morning, Night, Cleaning** – each with its own tasks and colour.
- **Switch** – tap the sun or moon; tap again, or use the back arrow or the system Back, to return to Main.
- **Light pages** – on the light Day list, task titles use a lighter weight than on the dark lists.
- **Dock** – the list scrolls underneath it, so text shows in the transparent corners beside the tab.
- **Wash transition** – the new colour spreads from the button.
- **Daily reset** – Morning and Night uncheck themselves at times you choose.
- **Fresh start** – killing the app reopens on Main; just minimising it keeps you on the list you were on.

### Cleaning dice
- **Roll** – picks one random tiny clean.
- **Fair** – favours tasks not done lately.
- **Bank** – tasks are counted, never used up.
- **Open list** – long-press the dice.

### Archive
- **Last 7 days** – shown first; older items fold away.
- **Send back** – the arrow returns a task to its list.

### Settings
Pull the list down from the top to open them. Settings and Archive always use the Main list's colours, whichever list you came from. Opening them from Morning or Night washes the page to Main's colours toward that list's button, and going back washes the list's colour out from the same button.
- **Archived notes** – opens the archive.
- **Reset times** – when Morning and Night uncheck.
- **Meditation timer length** – the default for the capsule.
- **Sleep countdown** – on or off, and your sleep time.
- **Lock the phone (Eye)** – on: the eye also locks the phone behind MONO; off: it only keeps the screen on, with no lock and no device-admin prompt.
- **Start the next task** – open it automatically after finishing one.
- **Stop at separators** – return to the list at a separator, or skip it.
- **Sound** and **haptics** – each on or off.
- **Theme** – dark or light.
- **Export** – saves lists, steps, separators, archive and settings to a `.txt` file.
- **Import** – add the file to the end of your lists, or overwrite them.

### Phone
- **Keep awake (eye)** – keeps the screen on, but locks the phone behind it. It turns on by itself when you start a task in Morning or Night, and stays on when you go back to the list or other screens. If the phone gets unlocked while it is on, coming back to the app locks it again. Rotating while it is on no longer flickers. Turn it off with the eye icon in focus, or the one in the list header.
- **Widgets** – a list widget (check and uncheck to-dos, with each to-do's steps shown under it and tickable too) and a focus widget; both always show the Main list and update live.
- **Sleep countdown** – an optional silent, top-ranked notification (also on the lock screen), one compact row: a small bed on the left, the time left until your sleep time on the right. It starts minimized and its colours step through four stages: black with a yellow time (more than 3 hours left); deep teal `#053837` with a yellow time and a lighter bed (3h to 2h); bright mint `#00E092` with black time and bed (2h to 1h); and the app's yellow with black time and bed for the last hour. After bedtime it says "Bedtime" for 3 hours. Needs the notification permission; it runs as a small foreground service, refreshed each minute and when the screen turns on.

## Build

```
gradle assembleDebug
```

Requires JDK 17, the Android SDK (API 34) and Gradle 8.5 (Gradle 9 does not work with the Android Gradle Plugin 8.2.2 used here; `./gradlew` just calls the system `gradle`); set `sdk.dir` in `local.properties`.
