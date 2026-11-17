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
- **Edit** – tap a task for its title, time estimate and steps; the rarely used parts (**Note** and **2-minute sessions**) sit folded under the steps and open when tapped (they start open when they already hold something). Native copy and paste work. Enter saves and closes.
- **Time estimate** – a free-text field under the title (a clock icon, then the text), with `mins` and `hrs` at its end. These two are switches: type `15`, tap `mins` and it lights up in the accent colour; nothing is pasted into the box, so the number stays easy to change later. Tap a lit one again to switch it off. The estimate shows on the right of the to-do, small and quiet, with the unit made singular or plural from the number ("1 hr", "15 mins"); hours plus minutes keep both parts ("2h 2mins"). It also shows under the title on the focus screen and on the right of the to-do in the list widget.
- **Type the time in the add bar** – put it at the end of the line: `Call the bank 15 mins`, `15min`, `15m`, `20 minutes`, `1 hr`, `2h`, `1.5 hours`, `1h30m`, `1 hr 30 mins` (saved as `1h 30mins`). A `-`, `|`, `@` or brackets before it are fine (`Email (20 min)`). A unit is required, so "Read chapter 3" stays as it is, and a one-letter unit must touch the number (`15m`, not `15 m`). The full list is in Settings → Shortcuts → Time formats.
- **Focus** – double-tap a task to start focus on it.
- **Insert where you choose** – tap the empty space just above or below any to-do or separator (including above the first and below the last). A thin inset line fades in there (yellow on the dark lists, near-black on the Day list) and the rows ease apart a little. Everything you add from the bar now goes into that spot: a to-do, several lines at once (in order, fading in one after another), or a separator via the dash button (the typed text becomes its label). When it is in, the line lets go and the keyboard hides. Tap the line again to deselect it. Back deselects it too (with the keyboard up, closing the keyboard does). It lets go straight away when you check or uncheck a box, open, drag, swipe or archive anything, and also by itself after 12 seconds of nothing happening (typing and adding keep it alive) or when you switch lists. With no spot chosen, adding still appends to the bottom. Taps near a row's edge choose the spot instead of opening that row; tap the middle of a row to open it. Tapping a gap also opens the keyboard (turn this off in Settings → Lists → "Keyboard on tapped spot"); while the keyboard rises, the list scrolls with it so the line always ends up in the middle of the space between the header and the add bar (short lists get temporary extra room so this works for them too), and it glides back when the keyboard closes or the spot is let go (unless you scrolled yourself). The add bar belongs to the bar: touches on it never choose a spot in the list under it. Pressing add happens in order: the new rows appear, then the keyboard closes, then the list glides back.
- **Add bar** – one task per line. Markdown is understood: bullets (`-`, `*`, `+`, `1.`), checklists (`- [ ]`, `- [x]`), and `**bold**`, `~~strike~~` and backticks are stripped. A ticked box or a ~~crossed out~~ item arrives already done.
- **Archive** – swipe left on a task, or tap the archive icon in the top bar to archive the whole list. Neither asks first: an Undo button (the same one as on the focus screen) floats above the add bar for 5 seconds and puts everything back exactly where it was, separators included. Archiving the whole list removes its separators until you undo.
- **Nothing archives itself.**

### Separators
- **Add** – the dash button in the add bar; anything typed becomes the label. A label is one line: once the add bar holds a line break the button greys out, and pressing it says a separator can't have a multi-line label (nothing typed is lost).
- **Move** – hold it anywhere and drag; it lifts into a card with room above and below.
- **Rename or remove** – tap it (Enter saves and closes), or swipe left to delete.
- **Time** – a separator can carry a time (a time of day such as `9:30 pm`, or any short text), shown right-aligned in the label's style. The editor looks like the to-do editor: the label, then a clock row with the time box and `am` / `pm` at its end, then `:00 :15 :30 :45` underneath. `am` / `pm` are switches (never pasted) and light up by themselves to match your phone's clock when you start typing a number; a time of 13 or more, or non-number text, switches them off. A minutes chip replaces the minutes already there (`9:30` + `:45` = `9:45`). When you reopen it, the box holds just the text and `am` / `pm` is the lit button; the list shows both together.
- **Time only** – a separator with a time and no label has no line: just the time, right-aligned in the accent yellow inside a thin rounded outline (in the list widget too).
- **Type the time in the add bar** – type the label and the time together and press the dash button: `Lunch 12:30`, `Lunch at 7pm`, `7pm` alone. Accepted: 12 hour (`7p`, `7a`, `7pm`, `7 pm`, `7:30 p.m.`, `630p`, `1130a`) and 24 hour (`19:00`, `7:30`, `19h`, `19h30`). 12 hour times are tidied to `6 pm` / `6:30 pm`. A bare number is not a time ("Section 7" stays a label).

### Focus
- **One task** – tap a task to see only it.
- **Next and previous** – swipe up or down, or tap the arrow.
- **Separators** – on Morning and Night, finishing the last task before a separator carries on into the next section, with a quiet rule and the separator's title (just the rule if it has none) in the middle of the top bar, below any camera cutout. Elsewhere, "Stop at separators" decides.
- **Scroll first** – when the next task is partly hidden, the list scrolls it to the middle before it opens.
- **Steps** – show every step, or just the next one.
- **Stuck menu** – try 2 minutes, break it into steps, or do something else first.
- **Done** – a wide pill (bigger in landscape); a short win moment, then the next task opens.
- **Double-tap** – double-tap empty space to start focus on the first open task, with the same grow-into-focus animation as tapping it.
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
- **Switch** – press and hold the add bar (while it is empty and the keyboard is down) and it turns into the list switcher: four glyphs (checklist = Main, sunrise = Morning, moon = Night, dice = Roll). Slide to one and let go; the bar fades back to normal as the page changes. Letting go on your current list, or without moving, does nothing. A tap on the bar still opens the keyboard. The bar's placeholder names the current list ("Add a task", "Add to Morning", "Add to Night", "Add a tiny clean"). The back arrow or the system Back return to Main.
- **Light pages** – on the light Day list, task titles use a lighter weight than on the dark lists. Separators there are a slightly darker grey so they stay readable on the pastel page.
- **Add bar** – one plain bar at the bottom (no raised tab), so nothing hides the list beyond that single bar.
- **Wash transition** – the new colour spreads from the bar.
- **Daily reset** – Morning and Night uncheck themselves at times you choose.
- **Fresh start** – killing the app reopens on Main; just minimising it keeps you on the list you were on.

### Cleaning dice
- **Roll** – picks one random tiny clean.
- **Fair** – favours tasks not done lately.
- **Bank** – tasks are counted, never used up.
- **Dice focus screen** – the bottom icons are Back (far left, returns to the list you came from), timer, Done, dice (another one) and Edit (far right, opens the Cleaning list).

### Archive
- **Last 7 days** – shown first; older items fold away.
- **Send back** – the arrow returns a task to its list.

### Settings
Pull the list down from the top to open them. Settings and Archive always use the Main list's colours, whichever list you came from. Opening them from Morning or Night washes the page to Main's colours toward that list's button, and going back washes the list's colour out from the same button.
- **Archived notes** – opens the archive.
- **Shortcuts → Time formats** – a quick reference of every time format the add bar understands, for to-dos and separators.
- **Reset times** – when Morning and Night uncheck.
- **Meditation timer length** – the default for the capsule.
- **Sleep countdown** – on or off, and your sleep time.
- **Lock the phone (Eye)** – on: the eye also locks the phone behind MONO; off: it only keeps the screen on, with no lock and no device-admin prompt.
- **Start the next task** – open it automatically after finishing one.
- **Stop at separators** – return to the list at a separator, or skip it.
- **Keyboard on tapped spot** – open the keyboard when you tap a gap to choose where to add (on by default; saved, and included in export/import).
- **Sound** and **haptics** – each on or off.
- **Theme** – dark or light.
- **Export** – saves lists, steps, separators, time estimates and separator times, archive and settings to a `.txt` file.
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
