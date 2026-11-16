# MONO

An Android to-do app that cuts list overwhelm by putting one task in front of you at a time.

## Download

[apk/MonoFocus-debug.apk](apk/MonoFocus-debug.apk) – open the file page on GitHub and tap Download, then install it on your phone.

## Features

### Tasks
- **Add** – type in the bar, one task per line.
- **Complete** – check it off; it stays in place, dimmed.
- **Reorder** – drag the handle.
- **Edit** – long-press for title, notes, steps and timer count.
- **Archive** – swipe left on a task, or archive everything from the top bar.
- **Restore** – unarchive from the Archive screen.
- **Nothing archives itself.**

### Focus
- **One task** – tap a task to see only it.
- **Steps** – show every step, or just the next one.
- **Stuck menu** – try 2 minutes, break it into steps, or do something else first.
- **Done** – a short win moment, then the next task.
- **Undo** – bring the last task back.

### Timer
- **2-minute timer** – start from the focus view.
- **Badges** – one per finished timer, on the task and in the list.
- **Editable** – change the count in the editor.

### Lists
- **Main, Morning, Night, Cleaning** – each with its own tasks and colour.
- **Switch** – tap the sun or moon; tap again to return.
- **Wash transition** – the new colour spreads from the button.

### Cleaning dice
- **Roll** – picks one random tiny clean.
- **Fair** – favours tasks not done lately.
- **Bank** – tasks are counted, never used up.
- **Open list** – long-press the dice.

### Phone
- **Keep awake** – the eye icon keeps the screen on, but locks the phone behind it.
- **Widgets** – a list widget and a focus widget.
- **Sounds and haptics** – soft tones, with a mute toggle.
- **Dark and light** – OLED black or light theme.

## Build

```
gradle assembleDebug
```

Requires the Android SDK (API 34); set `sdk.dir` in `local.properties`.
