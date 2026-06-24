# Clipboard Keyboard — Android Studio Setup Instructions

## Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 26+ (Android 8.0 Oreo or higher)
- Kotlin plugin (bundled with Android Studio)
- A physical Android device or emulator (API 26+)

---

## Step 1 — Open the Project

1. Launch Android Studio.
2. Click **File → Open**.
3. Navigate to the `ClipboardKeyboard` folder (this folder).
4. Click **OK** and wait for Gradle sync to finish.

---

## Step 2 — Gradle Sync

Android Studio will automatically sync Gradle and download all dependencies:
- Room (local database for clipboard storage)
- Material Components (Material 3 UI)
- Kotlin Coroutines
- Gson

If sync fails, check that you have an active internet connection and try **File → Sync Project with Gradle Files**.

---

## Step 3 — Run the App

1. Connect an Android device via USB (enable USB debugging in Developer Options), or start an emulator.
2. Click the **Run ▶** button (or press `Shift+F10`).
3. Select your device and click **OK**.

The app installs and opens the **Setup screen** which guides you through enabling the keyboard.

---

## Step 4 — Enable the Keyboard (on the device)

### 4a. Enable in Input Method Settings
1. In the Setup screen, tap **"Open Input Settings"**.
2. Find **Clipboard Keyboard** in the list and toggle it **ON**.
3. Confirm the security prompt.

### 4b. Select as Active Keyboard
1. Back in the Setup screen, tap **"Switch Keyboard"**.
2. Choose **Clipboard Keyboard** from the picker.
3. The keyboard is now active.

---

## How the Clipboard Queue Works

| Action | Result |
|--------|--------|
| Copy any text | Automatically added to the back of the queue |
| Tap **Paste Next** | Inserts the first item in the queue and removes it |
| Open **Clipboard panel** (📋 icon) | Browse, search, pin, or delete items |
| Tap an item in the panel | Pastes that specific item |
| Long-press an item | Enters multi-select mode |

---

## Project Structure

```
ClipboardKeyboard/
├── app/src/main/
│   ├── java/com/example/clipboardkeyboard/
│   │   ├── KeyboardIMEService.kt       ← Main InputMethodService (keyboard engine)
│   │   ├── KeyboardView.kt             ← QWERTY / symbols / emoji / clipboard UI
│   │   ├── ClipboardPanelView.kt       ← Clipboard panel (search, pin, delete)
│   │   ├── ClipboardAdapter.kt         ← RecyclerView adapter for clipboard items
│   │   ├── ClipboardItem.kt            ← Room entity
│   │   ├── ClipboardDao.kt             ← Room DAO (queries)
│   │   ├── ClipboardDatabase.kt        ← Room database singleton
│   │   ├── ClipboardRepository.kt      ← Data access layer
│   │   └── SetupActivity.kt            ← Onboarding/setup screen
│   └── res/
│       ├── layout/
│       │   ├── keyboard_view.xml        ← Root keyboard layout
│       │   ├── keyboard_row.xml         ← Single row of keys
│       │   ├── key_button.xml           ← Individual key button
│       │   ├── clipboard_panel.xml      ← Clipboard panel layout
│       │   ├── item_clipboard.xml       ← Clipboard list item
│       │   └── activity_setup.xml       ← Setup screen layout
│       ├── drawable/                    ← Vector icons
│       ├── values/                      ← Light theme colors, strings
│       ├── values-night/                ← Dark theme colors
│       └── xml/method.xml               ← IME metadata
```

---

## Customization

### Package Name
To change from `com.example.clipboardkeyboard`:
1. Right-click the package in Android Studio → **Refactor → Rename**.
2. Update `applicationId` in `app/build.gradle.kts`.
3. Update `android:name` in `AndroidManifest.xml`.

### App Name
Edit `app/src/main/res/values/strings.xml` → `app_name`.

### Colors / Theme
- Light: `app/src/main/res/values/colors.xml`
- Dark: `app/src/main/res/values-night/colors.xml`

### Add More Emoji Rows
In `KeyboardView.kt`, extend the `emojiRows` list with additional rows of emoji strings.

---

## Permissions Used

| Permission | Reason |
|------------|--------|
| `VIBRATE` | Haptic feedback on key press |

No internet permission is required. All clipboard data is stored **locally only** using Room (SQLite).

---

## Building a Release APK

1. In Android Studio: **Build → Generate Signed Bundle / APK**.
2. Choose **APK**.
3. Create or select a keystore.
4. Select **Release** build variant.
5. Click **Finish**.

The signed APK will be at `app/release/app-release.apk`.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Gradle sync fails | Check internet connection; try File → Sync |
| Keyboard not appearing | Make sure it is enabled AND selected in Step 4 |
| Keys not responding | Restart the device and re-select the keyboard |
| Dark mode not working | Ensure your device is running Android 10+ and dark mode is on |
| Clipboard not auto-capturing | Some Android 10+ devices restrict clipboard access from background; copied text can still be added manually via the clipboard panel |
