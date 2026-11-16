# MONO

An Android to-do app that cuts list overwhelm by putting one task in front of you at a time.

## Download

[apk/MonoFocus-debug.apk](apk/MonoFocus-debug.apk) – open the file page on GitHub and tap Download, then install it on your phone.

## Features

### Tasks
- **Add** – type in the bar, one task per line. The + appears only when there is text.
- **Complete** – check it off; it stays in place, dimmed. Morning separators are drawn in a soft grey so they stay quiet.
- **Reorder** – drag the handle, up or down.
- **Edit** – long-press for title, notes, steps and timer count. Native copy and paste work.
- **Archive** – swipe left on a task, or archive the whole list from the top bar (it asks first).
- **Nothing archives itself.**

### Separators
- **Add** – the dash button in the add bar; anything typed becomes the label.
- **Move** – hold it anywhere and drag.
- **Rename or remove** – tap it, or swipe left to delete.

### Focus
- **One task** – tap a task to see only it.
- **Next and previous** – swipe up or down, or tap the arrow.
- **Separators** – focus ends at a separator and returns you to the list.
- **Steps** – show every step, or just the next one.
- **Stuck menu** – try 2 minutes, break it into steps, or do something else first.
- **Done** – a short win moment, then the next task opens.
- **Double-tap** – double-tap empty space (including beside the list-button tab) to start focus on the first open task, with the same grow-into-focus animation as tapping it.
- **Landscape** – rotate for a flash-card view: the title is centred and large, steps are hidden.
- **Undo** – floats 10% from the top for 3 seconds after finishing. Leaving focus or moving to another task dismisses it.
- **Cleaning focus** – no stuck (?) button. Steps can be added in the editor only, not in focus.

### Timer
- **2-minute timer** – start from the focus view.
- **Badges** – one per finished timer, on the task and in the list.
- **Editable** – change the count in the editor.

### Lists
- **Main, Morning, Night, Cleaning** – each with its own tasks and colour.
- **Switch** – tap the sun or moon; tap again, or use the back arrow or the system Back, to return to Main.
- **Dock** – the list scrolls underneath it, so text shows in the transparent corners beside the tab.
- **Wash transition** – the new colour spreads from the button.
- **Daily reset** – Morning and Night uncheck themselves at times you choose.

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
- **Start the next task** – open it automatically after finishing one.
- **Stop at separators** – return to the list at a separator, or skip it.
- **Sound** and **haptics** – each on or off.
- **Theme** – dark or light.
- **Export** – saves lists, steps, separators, archive and settings to a `.txt` file.
- **Import** – add the file to the end of your lists, or overwrite them.

### Phone
- **Keep awake (eye)** – keeps the screen on, but locks the phone behind it. It turns on by itself when you start a task in Morning or Night, and stays on when you go back to the list or other screens. Turn it off with the eye icon in focus, or the one in the list header.
- **Widgets** – a list widget (check and uncheck, follows the open list) and a focus widget (always the Main list); both update live.

## Build

```
gradle assembleDebug
```

Requires the Android SDK (API 34) and Gradle 8.5 (Gradle 9 does not work with the Android Gradle Plugin 8.2.2 used here; `./gradlew` just calls the system `gradle`); set `sdk.dir` in `local.properties`.
