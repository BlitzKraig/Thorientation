# Android Display Rotation: Breakdown, Diagnosis, Repair & Architecture Blueprint

This document details how display auto-rotation was inadvertently locked via ADB, how the system state was diagnosed and restored to normal, and the reverse-engineering analysis of the Rotation Control APK explaining how to properly control rotation on the primary (top) display of dual-screen devices like the AYN Thor.

---

## 1. How Auto-Rotate Broke

During low-level testing of Android's window rotation commands via ADB, the following commands were executed:
```bash
adb shell cmd window user-rotation -d 0 lock 1
adb shell cmd window user-rotation -d 0 lock 0
```

### What Happened Internally
1. **WindowManager State Locking**: In Android's WindowManager service (`DisplayRotation.java`), the `user-rotation ... lock` command changes `mUserRotationMode` from `USER_ROTATION_FREE` to `USER_ROTATION_LOCKED`.
2. **Sensor Listener Detachment**: When `mUserRotationMode` is locked, Android unregisters the hardware sensor listener (`WindowOrientationListener.mEnabled = false`). Even though physical motion continues, Android stops delivering orientation sensor events to the rotation policy.
3. **Fixed to User Rotation Flag**: The display rotation policy set `mFixedToUserRotation = true`. In this state, any app or launcher attempting to read or adapt to orientation changes is pinned to the user-locked rotation (`ROTATION_90`).
4. **Settings UI Desync**: Changing the "Auto-rotate" toggle in Android Settings or toggling `Settings.System.ACCELEROMETER_ROTATION` does not reliably clear the low-level `cmd window` user-rotation lock override on secondary displays or customized OEM WindowManager implementations.

---

## 2. Diagnosis and Recovery

### Diagnosis
Running `adb shell dumpsys window displays` revealed:
```text
DisplayRotation:
  mCurrentAppOrientation=SCREEN_ORIENTATION_UNSPECIFIED
  mRotation=1
  WindowOrientationListener:
    mEnabled=false   <-- Sensor listener turned off by system
  mUserRotationMode=USER_ROTATION_LOCKED
  mUserRotation=ROTATION_90
  mFixedToUserRotation=true   <-- Pinned to landscape
```

### Recovery Sequence
To completely reset Android's WindowManager and release all overrides back to dynamic sensor control, the following commands were executed:

```bash
# 1. Release the low-level user rotation lock on all displays
adb shell cmd window user-rotation -d 0 free
adb shell cmd window user-rotation -d 1 free

# 2. Reset the fixed-to-user-rotation policy back to system default
adb shell cmd window fixed-to-user-rotation -d 0 default

# 3. Clear any display-specific geometry or rotation overrides
adb shell cmd window reset -d 0

# 4. Ensure system-level accelerometer rotation is enabled
adb shell settings put system accelerometer_rotation 1
```

### Verification
A subsequent dump of `dumpsys window displays` confirmed:
- `mUserRotationMode=USER_ROTATION_FREE`
- `mFixedToUserRotation=false`
- Live transitions logged as the device was tilted:
  ```text
  15:13:14 ROTATION_90  -> ROTATION_180
  15:13:16 ROTATION_180 -> ROTATION_0   (Portrait)
  15:13:18 ROTATION_0   -> ROTATION_90   (Landscape)
  ```

---

## 3. Reverse-Engineering Rotation Control APK

The APK `org.crape.rotationcontrol` was decompiled and analyzed to find how it successfully forces orientation on the top screen even when applications (and launchers) resist.

### Key Architectural Findings

#### A. "Guard Mode" Uses `TYPE_APPLICATION_OVERLAY`
Rotation Control does not rely on `Settings.System.USER_ROTATION` or root commands. Instead, it relies on an overlay window in a foreground service:

1. **Window Parameters (`RotationControlViewParam`)**:
   ```java
   super(
       0, 0, // 0x0 width and height
       WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // 2038
       WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, // 24
       PixelFormat.TRANSLUCENT // -2
   );
   this.gravity = Gravity.TOP; // 48
   this.screenOrientation = requestedOrientation; // e.g. ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
   ```
2. **Top-Level Window Hierarchy**:
   Because `TYPE_APPLICATION_OVERLAY` windows sit above normal application windows in the window manager z-order, the WindowManager inspects the topmost visible window for its orientation requirements. Setting `params.screenOrientation` on this overlay forces the display to rotate to match it.
3. **Auto-Rotate Prerequisite (`accelerometer_rotation = 1`)**:
   Right before calling `windowManager.addView(view, params)` or `updateViewLayout`, Rotation Control explicitly executes:
   ```java
   Settings.System.putInt(contentResolver, "accelerometer_rotation", 1);
   ```
   If `accelerometer_rotation` is `0`, Android's WindowManager locks orientation to user rotation and ignores non-fullscreen overlay orientation requests. Enabling auto-rotation allows the overlay's requested orientation to govern the screen.

#### B. Direct `WindowManager` vs `createDisplayContext`
- For Display 0 (Top / Primary Screen), the view must be added to the service's own `WindowManager` (`getSystemService(Context.WINDOW_SERVICE) as WindowManager`).
- Calling `createDisplayContext(display)` on Display 0 in Android 12+ generates a non-UI display context that lacks proper token binding for system overlays.

---

## 4. MultiScreenManager Implementation Plan

To adopt this proven architecture in [MultiScreenManager](file:///t:/Projects/MISC/MultiScreenManager):

1. **Update `ScreenOverlayService.kt`**:
   - For Display 0, bind the overlay view directly using the service's primary `WindowManager`.
   - Set overlay dimensions to `0x0`, gravity to `Gravity.TOP`, flags to `FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE`.
   - Before adding/updating the overlay layout, set `Settings.System.ACCELEROMETER_ROTATION = 1`.
   - Use `updateViewLayout(view, params)` on existing view instances for smooth transitions instead of tearing down and recreating views.
   - When clearing/resetting or setting `ScreenOrientation.AUTO`, remove the overlay view.
2. **Update `RotationEngine.kt`**:
   - Stop delegating Display 0 exclusively to `SettingsEngine.setOrientation` (which only writes the ignored `USER_ROTATION` setting).
   - Route Display 0 orientation changes through `OverlayEngine`, ensuring consistent behavior across both screens.
