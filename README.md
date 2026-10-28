# Nova Keyboard

A modern, private Android keyboard written from scratch in Kotlin.
It re-creates the *kind* of features found in feature-rich keyboards (themes, shortcuts, clipboard, emoji,
stickers, voice, OCR, text styles, calculator) with new code, new themes, new sounds and a Material 3
settings app. No code or assets were copied from any other keyboard.

## Build

1. Install Android Studio (Koala or newer).
2. **File > Open** and choose this folder. Let Gradle sync (it creates the Gradle wrapper for you).
3. Press **Run**, or **Build > Build APK(s)** for an installable file.
4. On the phone: open *Nova Keyboard*, tap **Open keyboard settings**, switch it on, then **Choose Nova Keyboard**.

Unit tests for the logic: `./gradlew test` (calculator, text helpers, text styles, layouts).

## Privacy

- Manifest requests only `VIBRATE`. **No internet, contacts, accounts, microphone or overlay permission.**
- No analytics, crash reporters, ads or social SDKs.
- Learned words, clipboard history, shortcuts and themes live in the app's private storage.
- Voice typing hands off to the phone's own speech recogniser, so this app never touches the microphone.
- Text from image uses ML Kit's on-device model (bundled, works offline).
- Backups are a single JSON file saved wherever you choose.
- Clipboard history is not recorded in password fields.

## Features

| Area | What you get |
|---|---|
| Layouts | English QWERTY, French AZERTY, Arabic (RTL), symbols (2 pages), numpad, calculator, optional number row |
| Typing | Fast two-thumb typing, long-press alternatives (accents, currency, Arabic-Indic digits), backspace repeat, swipe the space bar to move the caret, double-space period, auto-capitalise, one-handed mode, adjustable height |
| Suggestions | Offline completions and next-word guesses, learns from you (never in password fields), editable personal dictionary |
| Themes | 9 built-in themes, full designer with live preview (colours, corner radius, gaps, opacity, outline, font, photo background), share and import themes as text |
| Shortcuts | Type a code, press space, get the full phrase |
| Clipboard | History with pinning, one-tap clear |
| Emoji | 8 categories plus recents |
| Stickers | Import your own images and send them to apps that accept images from keyboards |
| Text styles | 17 Unicode styles (bold, script, fraktur, circled, small caps, upside-down ...) |
| Voice | Via the system recogniser (en, fr, ar) |
| OCR | Read text from a photo or screenshot, offline |
| Calculator | Calculator layout; type an expression and press `=` |
| Sound and haptics | 4 synthesised key sounds (no audio files), volume, vibration strength |
| Bottom row | Choose and reorder the keys beside the space bar |
| Backup | Local export and import of settings, themes, shortcuts, dictionary |

## Differences from older keyboards, on purpose

- Cloud backup and web image search are gone (they need accounts or internet). Backup is a local file.
- Floating keyboard and Pinyin/Hangul input are not included yet.
- The bundled word lists are small starter lists. For better suggestions, replace
  `app/src/main/assets/words_<lang>.txt` with an open-licensed frequency list (one word per line, most common first).

## Project layout

```
app/src/main/java/app/novakeys/
  ime/        NovaIME (the keyboard service), Panels (emoji, clipboard, tools, stickers), TextUtil
  keyboard/   KeyboardView (drawing + touch), Layouts, KeyIcons, models
  theme/      KeyboardTheme, built-in palettes, ThemeStore
  data/       Prefs, Predictor, shortcuts, clipboard, stickers, emoji, backup
  feature/    key sounds + haptics, voice, OCR, text styles, calculator
  ui/         Compose settings app
```

## Not yet verified

This code was written without access to the Android SDK, so it has not been compiled or run on a device.
The pure logic is unit tested. If Android Studio reports an error on first sync, it will point to the exact line.
