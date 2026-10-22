# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-10-22

### Added
- **Master Power Switch**:
  - Main screen on/off switch providing instant, global control over orientation management without revoking system permissions.
  - Automatically manages background overlays and persistence services, and updates Quick Settings tiles to reflect active status.
- **Background Persistence via Accessibility Service**:
  - Optional Accessibility Service (`OrientationAccessibilityService`) to maintain secondary screen orientation across task switcher dismissals.
  - Dedicated UI toggle card with full support for programmatic deactivation (`disableSelf()`).
- **Start at Boot Preference & Toggle**:
  - UI switch to control whether Thorientation automatically launches and restores display rotations on system reboot (`BootReceiver`).
- **Enhanced Quick Settings & Presets**:
  - Dual Preset Quick Settings tile now features direct preset selection dialog support in addition to tap cycling.
- **Modern Adaptive App Icon**:
  - Clean light 'T' motif with subtle rotation swooshes and dark metallic background.

### Fixed
- Resolved potential Foreground Service crashes and state desynchronization during rapid Quick Settings tile interaction.
- Resolved orientation reset when swiping Thorientation out of the Android Recents / App Switcher.

## [1.0.0] - 2026-10-22

### Added
- **Independent Dual-Screen Orientation Control**:
  - Full, isolated orientation management for both the primary Top Screen (Display 0, 6.0" AMOLED) and secondary Bottom Screen (Display 1, 3.92" AMOLED) on the AYN Thor.
  - Supports Portrait (0°), Landscape (90°), Reverse Portrait (180°), Reverse Landscape (270°), and dynamic Sensor Auto-Rotate.
- **Quick Settings Shade Tiles**:
  - **Top Screen Tile**: Displays live active top screen rotation; tapping triggers a quick selection dialog.
  - **Bottom Screen Tile**: Displays live secondary screen rotation with instant popup selection.
  - **Dual Preset Tile**: One-tap toggle cycling through popular dual-display presets:
    - *3DS / DS Gaming*: Top in Landscape (90°) + Bottom in Portrait (0°)
    - *Dual Landscape*: Both screens in Landscape (90°)
    - *Tate / Arcade Mode*: Both screens in Portrait (0°) for vertical shooters & pinball
    - *Dual Auto*: Sensor-based auto-rotation on both displays
- **Fast In-Game Popups**:
  - Non-intrusive floating dialog activity triggered from Quick Tiles for rapid orientation switching without switching apps or exiting active games.
- **Companion Display Utilities**:
  - **Bottom Screen Standby (AMOLED Blackout)**: Black out and deactivate the bottom display to conserve battery and eliminate unwanted heat during single-screen play.
  - **Secondary Screen App Launcher**: Launch any installed application (browser, guides, Discord, YouTube) directly onto the secondary bottom display.
  - **Persistent Notification Controls**: Quick-action notification controls directly in the Android pull-down shade.
- **Zero Root / Zero ADB Architecture**:
  - Operates without root, wireless debugging, Shizuku, or background daemons.
  - Employs standard Android `TYPE_APPLICATION_OVERLAY` combined with `WRITE_SETTINGS` system APIs.
- **Boot Persistence**:
  - Automatically restores selected orientation configurations after device restart.
- **CI/CD Automation**:
  - GitHub Actions workflow for automated release packaging and publishing on GitHub Releases.
