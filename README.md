# MONO

An Android to-do app that cuts list overwhelm by putting one task in front of you at a time.

## Download

[apk/MonoFocus-debug.apk](apk/MonoFocus-debug.apk) – open the file page on GitHub and tap Download, then install it on your phone.

## Features

### Tasks
- **Add** – type in the bar, one task per line. The + appears only when there is text.
- **Complete** – check it off; it stays in place, dimmed.
- **Steps** – a task's steps show under it in every list, and can be ticked right there.
- **Reorder** – drag the handle, up or down.
- **Edit** – long-press for title, notes, steps and timer count. Native copy and paste work. Enter saves and closes.
- **Archive** – swipe left on a task, or archive the whole list from the top bar (it asks first).
- **Nothing archives itself.**

### Separators
- **Add** – the dash button in the add bar; anything typed becomes the label.
- **Move** – hold it anywhere and drag; it lifts into a card with room above and below.
- **Rename or remove** – tap it, or swipe left to delete.

### Focus
- **One task** – tap a task to see only it.
- **Next and previous** – swipe up or down, or tap the arrow.
- **Separators** – on Morning and Night, finishing the last task before a separator carries on into the next section, with a quiet rule and the separator's title (just the rule if it has none). Elsewhere, "Stop at separators" decides.
- **Scroll first** – when the next task is partly hidden, the list scrolls it to the middle before it opens.
- **Steps** – show every step, or just the next one.
- **Stuck menu** – try 2 minutes, break it into steps, or do something else first.
- **Done** – a wide pill (bigger in landscape); a short win moment, then the next task opens.
- **Double-tap** – double-tap empty space (including beside the list-button tab) to start focus on the first open task, with the same grow-into-focus animation as tapping it.
- **Landscape** – rotate for a flash-card view: the title is centred and large, steps are hidden.
- **Undo** – floats 10% from the top for 3 seconds after finishing. Leaving focus or moving to another task dismisses it.
- **Cleaning focus** – no stuck (?) button. Steps can be added in the editor only, not in focus.

### Timer
- **Meditation timer** – tasks that mention meditate, focus, mindful, breathe and the like get a quiet "5 min" capsule on their focus screen (length set in Settings). While running it shows the countdown with +1m, +5m and cancel; when it ends the capsule settles to white and grey and a soft bell rings three times. It keeps running if you leave the screen.
- **2-minute timer** – start from the focus view.
- **Badges** – one per finished timer, on the task and in the list.
- **Editable** – change the count in the editor.

### Lists
- **Main, Morning, Night, Cleaning** – each with its own tasks and colour.
- **Switch** – tap the sun or moon; tap again, or use the back arrow or the system Back, to return to Main.
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
Pull the list down from the top to open them.
- **Archived notes** – opens the archive.
- **Reset times** – when Morning and Night uncheck.
- **Meditation timer length** – the default for the capsule.
- **Sleep countdown** – on or off, and your sleep time.
- **Start the next task** – open it automatically after finishing one.
- **Stop at separators** – return to the list at a separator, or skip it.
- **Sound** and **haptics** – each on or off.
- **Theme** – dark or light.
- **Export** – saves lists, steps, separators, archive and settings to a `.txt` file.
- **Import** – add the file to the end of your lists, or overwrite them.

### Phone
- **Keep awake (eye)** – keeps the screen on, but locks the phone behind it. It turns on by itself when you start a task in Morning or Night, and stays on when you go back to the list or other screens. If the phone gets unlocked while it is on, coming back to the app locks it again. Rotating while it is on no longer flickers. Turn it off with the eye icon in focus, or the one in the list header.
- **Widgets** – a list widget (check and uncheck) and a focus widget; both always show the Main list and update live.
- **Sleep countdown** – an optional silent, top-ranked notification (also on the lock screen) showing the time left until your sleep time as large light numerals beside a small grey bed. It is the app's own near-black with off-white numerals, and in the last 3 hours the numerals drift gradually toward the app's yellow. After bedtime it says "Bedtime" for 3 hours. Needs the notification permission; it runs as a small foreground service, refreshed each minute and when the screen turns on.

## Build

```
gradle assembleDebug
```

Requires JDK 17, the Android SDK (API 34) and Gradle 8.5 (Gradle 9 does not work with the Android Gradle Plugin 8.2.2 used here; `./gradlew` just calls the system `gradle`); set `sdk.dir` in `local.properties`.
