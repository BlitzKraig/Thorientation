# Thorientation 🎮📱

**Thorientation** is a specialized dual-display orientation controller and screen management utility engineered specifically for the **AYN Thor** (and dual-screen Android handhelds).

It allows independent, seamless orientation management of both the primary top screen (6.0" AMOLED) and secondary bottom screen (3.92" AMOLED), featuring dynamic **Quick Settings Tiles**, quick selection popups, synchronized dual-screen presets, persistent notification controls, and background persistence.

> **Note**: This tool was built as a personal fix. I'm simply sharing it in case anyone else finds it useful. It was "vibe coded", and I'm not interested in debating that. FWIW, I am a professional software engineer with over a decade of experience, but have very little free personal time to build things. If anyone has an issue with vibe coding, please feel free to ignore this project and move on.

---

## ✨ Features

### 1. Independent Multi-Display Orientation Control
* **Top Screen (Display 0)** & **Bottom Screen (Display 1)** can be rotated independently to:
  * 📱 **Portrait (0°)**
  * 🖥️ **Landscape (90°)**
  * 🔄 **Reverse Portrait (180°)**
  * 🌀 **Auto-Rotate (Sensor)**
* *(Note: Reverse Landscape / 270° is intentionally excluded due to hardware/OS driver limitations on the AYN Thor).*

### 2. Quick Settings Tiles (Notification Shade)
Add the tiles to your AYN Thor notification pull-down shade for instant access anytime while in-game without switching out of your active app:
* **Top Screen Tile**: Displays current orientation (e.g., *"Landscape"*). Tapping opens a quick selection dialog.
* **Bottom Screen Tile**: Displays current secondary screen rotation (e.g., *"Portrait"*). Tapping opens a quick selection dialog.
* **Presets Tile**: Displays current active preset (e.g., *"Landscape"* or *"Off"*). Tapping opens a quick selection dialog to choose any dual-screen preset instantly.

### 3. Synchronized Dual-Screen Presets
Quickly configure both displays simultaneously with one tap from the main dashboard, quick tiles, or persistent notification:
* **Dual Landscape**: Both screens set to Landscape (90°)
* **Dual Portrait**: Both screens set to Portrait (0°)
* **Dual Reverse Portrait**: Both screens set to Reverse Portrait (180°)
* **Dual Auto**: Both screens set to sensor auto-rotation

### 4. Master Power Switch
* One-tap master switch to activate or suspend all orientation management globally.
* Pauses overlays and releases display overrides without revoking any system permissions.
* Quick Settings tiles automatically reflect active / paused state.

### 5. Persistent Notification Controls
* Optional persistent notification in the Android shade with customizable one-tap action buttons.
* Choose the quick action target: **Presets**, **Top Screen**, or **Bottom Screen**.
* Expanding the notification exposes direct control buttons for rapid adjustment without opening the app.

### 6. Background Persistence (Accessibility Service)
* Optional Accessibility Service (`OrientationAccessibilityService`) ensuring secondary display orientation persists even if Thorientation is cleared or swiped out of the Android App Switcher (Recents).
* Dedicated UI switch with full support for programmatic deactivation from within the app.
* Zero CPU / battery overhead — does not monitor accessibility events.

### 7. Persistent Boot Restoration (Start at Boot)
* Configurable toggle to automatically re-apply your saved display rotations and background services upon device restart.

### 8. Thor OEM "Force Landscape" Conflict Detection
* Real-time detection and in-app warning banner for AYN Thor's native "Force Landscape" OS setting.
* Guides you to disable the native setting if it is overriding and blocking portrait display rotation.

### 9. Zero Root, Zero ADB, Offline Architecture
* **No Root, No ADB, No Shizuku Required**: Orientation is enforced seamlessly through Android's `TYPE_APPLICATION_OVERLAY` window architecture combined with `WRITE_SETTINGS` for primary display synchronization.
* Works completely offline with zero telemetry or background daemons.

---

## 🚀 Installation & Setup

### Direct APK Download
1. Head over to the [GitHub Releases](https://github.com/BlitzKraig/Thorientation/releases) page.
2. Download the latest `Thorientation-v1.1.0.apk`.
3. Open and install it on your AYN Thor (allow installing unknown apps if prompted).

### Install via ADB
Alternatively, connect your AYN Thor via USB or wireless debugging and run:
```bash
adb install -r Thorientation-v1.1.0.apk
```

---

## ⚙️ Permissions Setup

When first opening **Thorientation**, grant the requested permissions:

### Required Permissions
1. **Display over other apps (Overlay)**: Required to maintain independent orientation on secondary displays and enforce rotation.
2. **Modify system settings**: Required to synchronize system orientation and sensor auto-rotate for the primary display.

### Optional Permissions
3. **Post Notifications**: Required only if you enable **Persistent Notification** controls (Android 13+).
4. **Accessibility Service**: Required only if you enable **Background Persistence** to survive "Close All" in the Android App Switcher.

---

## 🛠️ Adding Quick Settings Tiles

1. Swipe down twice from the top of the screen to fully open the Quick Settings shade.
2. Tap the **Edit (Pencil)** icon.
3. Scroll down through available tiles to find:
   * **Top Screen**
   * **Bottom Screen**
   * **Presets**
4. Drag them into your active tiles section.

---

## 💡 Tips & Troubleshooting

* **Displays refusing to rotate to Portrait?**
  * Check if Thor's native **Force Landscape** tile is enabled in your Quick Settings. When active, Thor's OS forcibly locks all screens to landscape. Simply turn off "Force Landscape" in the notification shade.
* **Orientations reset when closing apps from Recents?**
  * Turn on **Background Persistence** in the Thorientation dashboard under *Startup & Persistence*. This enables the lightweight accessibility service which prevents Android's task killer from resetting the secondary display overlay.
* **Need to temporarily release orientation control?**
  * Toggle the **Master Switch** at the top of the Thorientation dashboard. Overlays will be released immediately and can be re-enabled whenever you're ready.
