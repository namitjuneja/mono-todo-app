# MONO

An Android to-do app that cuts list overwhelm by putting one task in front of you at a time.

## Download

[apk/MonoFocus-debug.apk](apk/MonoFocus-debug.apk) – open the file page on GitHub and tap Download, then install it on your phone.

## Features

### Tasks
- **Add** – type in the bar, one task per line. The + appears only when there is text.
- **Complete** – check it off; it stays in place, dimmed.
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
- **Undo** – floats near the top for a few seconds after finishing.

### Timer
- **2-minute timer** – start from the focus view.
- **Badges** – one per finished timer, on the task and in the list.
- **Editable** – change the count in the editor.

### Lists
- **Main, Morning, Night, Cleaning** – each with its own tasks and colour.
- **Switch** – tap the sun or moon; tap again, or use the back arrow, to return.
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
- **Keep awake** – the eye icon keeps the screen on, but locks the phone behind it.
- **Widgets** – a list widget (check and uncheck) and a focus widget; both update live.

## Build

```
gradle assembleDebug
```

Requires the Android SDK (API 34) and Gradle 8.5; set `sdk.dir` in `local.properties`.
