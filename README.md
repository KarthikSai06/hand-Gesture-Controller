# GestureControl Android App

A professional Android app that uses the **front camera + Google MediaPipe** to recognize hand gestures and inject them as system-wide touch actions via Accessibility Services.

---

## 🚀 Quick Setup

### Step 1 — Download the MediaPipe Model

> [!IMPORTANT]
> The model file is **not included** due to size. You must download it manually.

1. Download from:  
   `https://storage.googleapis.com/mediapipe-models/gesture_recognizer/gesture_recognizer/float16/latest/gesture_recognizer.task`
2. Place the file at:  
   `app/src/main/assets/gesture_recognizer.task`  
   *(create the `assets/` folder if it doesn't exist)*

### Step 2 — Open in Android Studio

1. Open **Android Studio** (Hedgehog 2023.1.1 or later recommended)
2. **File → Open** → select the `GestureControl` folder
3. Wait for Gradle sync to complete (downloads ~500 MB of dependencies on first run)
4. **Build → Make Project** — should compile with 0 errors

### Step 3 — Run on Device

> **Physical device required** — The Accessibility Service and foreground camera don't work on emulators.

1. Enable **Developer Options** and **USB Debugging** on your Android device
2. Run the app via Android Studio
3. Grant **Camera** and **Notification** permissions when prompted
4. Navigate to **Settings → Accessibility → Installed services → GestureControl** and enable it
5. Return to the app — all permission cards should show ✅

---

## 🎮 Gesture Actions

| Gesture | Action |
|---|---|
| 👍 Thumbs Up | Double Tap at screen center |
| ☝️ Pointing Up | Swipe Up (full screen height) |

---

## 📱 App Screens

| Screen | Description |
|---|---|
| **Home** | Permission status, service toggle, gesture guide |
| **Test** | Live camera preview + real-time gesture verification |
| **Settings** | Toggle gestures, adjust confidence & cooldown |

---

## ⚙️ Architecture

```
Front Camera (CameraX)
    → GestureRecognizerHelper (MediaPipe LIVE_STREAM)
    → CameraForegroundService (maps gestures → actions)
    → GestureAccessibilityService (dispatches system gestures)
    → GestureEventBus (broadcasts to UI for real-time display)
```

---

## 📋 Requirements

- Android 9.0+ (API 28)
- Front-facing camera
- Accessibility Service permission
- Camera + Notification runtime permissions

---

## 🔧 Troubleshooting

| Problem | Solution |
|---|---|
| "Model not found" crash | Place `gesture_recognizer.task` in `app/src/main/assets/` |
| Gestures not injecting | Enable Accessibility Service in Android Settings |
| Camera black screen | Grant Camera permission; use physical device |
| No notification | Grant Notification permission (Android 13+) |
