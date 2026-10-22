# Thorientation 🎮📱

**Thorientation** is a specialized dual-display orientation controller and screen management utility engineered specifically for the **AYN Thor** (and dual-screen Android handhelds).

It allows independent, seamless orientation management of both the primary top screen (6.0" AMOLED) and secondary bottom screen (3.92" AMOLED), featuring dynamic **Quick Settings Tiles**, quick selection popups, dual-screen presets, and companion power-saving tools.

> **Note**: This tool was built as a personal fix. I'm simply sharing it in case anyone else finds it useful. It was "vibe coded", and I'm not interested in debating that. FWIW, I am a professional software engineer with over a decade of experience, but have very little free personal time to build things. If anyone has an issue with vibe coding, please feel free to ignore this project and move on.

---

## ✨ Features

### 1. Independent Multi-Display Orientation Control
* **Top Screen (Display 0)** & **Bottom Screen (Display 1)** can be rotated independently to:
  * 📱 **Portrait (0°)**
  * 🖥️ **Landscape (90°)**
  * 🔄 **Reverse Portrait (180°)**
  * 🔄 **Reverse Landscape (270°)**
  * 🌀 **Auto-Rotate (Sensor)**

### 2. Quick Settings Tiles (Notification Shade)
Add the tiles to your AYN Thor notification pull-down shade for instant access anytime while in-game:
* **Top Screen Tile**: Displays the current orientation (e.g., *"Landscape"*). Tapping opens a quick selection sheet to change orientation on the fly without exiting your game.
* **Bottom Screen Tile**: Displays current secondary screen rotation (e.g., *"Portrait"*). Tapping opens the bottom-screen orientation selector.
* **Dual Preset Tile**: One-tap toggle that cycles through popular dual-screen arrangements:
  * **3DS / DS Gaming**: Top in Landscape (90°) + Bottom in Portrait (0°)
  * **Dual Landscape**: Both screens in Landscape (90°)
  * **Tate / Arcade Mode**: Both screens in Portrait (0°) for vertical arcade shooters & pinball
  * **Dual Auto**: Sensor-based rotation on both displays

### 3. Lightweight Overlay & Settings Engine
* **No Root, No ADB, No Shizuku Required**: Orientation is enforced seamlessly through Android's `TYPE_APPLICATION_OVERLAY` window architecture combined with `WRITE_SETTINGS` for primary display synchronization.
* Works completely offline and persists without requiring running background daemons or wireless debugging pairing codes.

### 4. Built-in Screen Management Tools
* ⚡ **Master Power Switch**: One-tap master switch to activate or suspend all orientation management on the fly without revoking permissions.
* 🛡️ **Background Persistence**: Optional accessibility service integration ensuring secondary display orientation persists even if Thorientation is swiped out of the Android App Switcher (Recents).
* 🔋 **Bottom Screen Standby (AMOLED Blackout)**: Turn off/dim the bottom display completely when playing single-screen titles to save battery and eliminate unwanted heat.
* 🚀 **Launch App on Bottom Display**: Browse installed apps (browsers, Discord, YouTube, companion guides) and launch them directly onto the bottom screen using Android's multi-display windowing APIs.
* 🔄 **Persistent Boot Restoration**: Automatically restores your chosen screen rotations after an OS reboot (with optional start-at-boot toggle).

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

When first opening **Thorientation**, you will be prompted to grant two standard Android permissions:
1. **Display over other apps (Overlay)**: Enables independent orientation lock for secondary screens.
2. **Modify system settings**: Synchronizes system orientation for the primary display.

---

## 🛠️ Adding Quick Settings Tiles
1. Swipe down twice from the top of the screen to fully open the Quick Settings shade.
2. Tap the **Edit (Pencil)** icon.
3. Scroll down to find:
   * **Top Screen**
   * **Bottom Screen**
   * **Dual Preset**
4. Drag them into your active tiles section.
