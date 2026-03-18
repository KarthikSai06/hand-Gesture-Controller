<div align="center">

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/Min%20SDK-API%2028-blue?style=for-the-badge"/>
<img src="https://img.shields.io/badge/ML%20Model-MediaPipe%20Gesture%20Recognizer-FF6D00?style=for-the-badge&logo=google&logoColor=white"/>
<img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white"/>
<img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge"/>

# ✋ GestureControl

**A fully on-device, hands-free Android controller powered by the MediaPipe Gesture Recognizer.**

GestureControl transforms any Android device into a gesture-driven interface. By running a production-grade machine learning model entirely on-device — with zero cloud dependency — it delivers real-time hand gesture recognition that maps to system-wide actions: taps, swipes, navigation, and custom shortcuts.

</div>

---

## 📸 Screenshots

| Home & Permissions | Test Mode | Action Mapping | Settings |
| :---: | :---: | :---: | :---: |
| ![Home](https://github.com/KarthikSai06/hand-Gesture-Controller/blob/main/screenshots/home.jpeg?raw=true) | ![Test Mode](https://github.com/KarthikSai06/hand-Gesture-Controller/blob/main/screenshots/test_mode.jpeg?raw=true) | ![Action](https://github.com/KarthikSai06/hand-Gesture-Controller/blob/main/screenshots/action.jpeg?raw=true) | ![Settings](https://github.com/KarthikSai06/hand-Gesture-Controller/blob/main/screenshots/settings.jpeg?raw=true) |

---

## 🤖 The ML Model — MediaPipe Gesture Recognizer

At the core of GestureControl is Google's **MediaPipe Gesture Recognizer** (`.task` bundle), a state-of-the-art multi-stage ML pipeline purpose-built for mobile inference.

### How It Works

```
Camera Frame  ──►  Hand Detection  ──►  21 Landmark Localization  ──►  Gesture Classification  ──►  Action
(CameraX)          (BlazePalm)          (Hand Landmark Model)          (Custom Classifier Head)
```

| Stage | Model | Output |
|---|---|---|
| **Palm Detection** | BlazePalm (lightweight CNN) | Bounding box of the hand region |
| **Hand Landmark Estimation** | 21-keypoint regression network | 3D (x, y, z) coordinates per landmark |
| **Gesture Classification** | Trained classifier head | Gesture category + confidence score |

### Why On-Device?

| Characteristic | Detail |
|---|---|
| **Runtime** | MediaPipe Tasks SDK — TFLite delegate |
| **Inference latency** | < 30 ms on mid-range devices |
| **Privacy** | All processing is local — no data leaves the device |
| **Connectivity** | Works fully offline |
| **Model format** | `.task` bundle (optimised FlatBuffer graph) |

The model runs on the device's **GPU delegate** when available, automatically falling back to the CPU — ensuring it performs reliably across the entire Android device spectrum.

---

## 📱 Mobile Integration Architecture

GestureControl is built around Android's native services to achieve deep, system-level gesture injection without root access.

```
┌─────────────────────────────────────────────────────────────┐
│                        Android OS                           │
│                                                             │
│  ┌──────────────┐    ┌───────────────────┐                  │
│  │  CameraX API │───►│  Foreground       │                  │
│  │  (Analysis   │    │  Service          │                  │
│  │   UseCase)   │    │  (Background      │                  │
│  └──────────────┘    │   Lifecycle Mgr)  │                  │
│                      └────────┬──────────┘                  │
│                               │                             │
│                      ┌────────▼──────────┐                  │
│                      │  MediaPipe        │                  │
│                      │  Gesture          │                  │
│                      │  Recognizer       │                  │
│                      │  (On-Device ML)   │                  │
│                      └────────┬──────────┘                  │
│                               │ Gesture Event               │
│            ┌──────────────────┼──────────────────┐          │
│            ▼                  ▼                  ▼          │
│  ┌──────────────────┐  ┌─────────────┐  ┌──────────────┐   │
│  │ Accessibility    │  │  SharedFlow │  │  Quick       │   │
│  │ Service          │  │  Event Bus  │  │  Settings    │   │
│  │ (GestureDesc-    │  │  (Reactive  │  │  Tile        │   │
│  │  ription API)    │  │   UI Layer) │  │  (System UI) │   │
│  └──────────────────┘  └─────────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Key Integration Points

| Android API | Role |
|---|---|
| **CameraX** `ImageAnalysis` | Feeds low-latency camera frames directly to the ML pipeline |
| **MediaPipe Tasks SDK** | Runs the gesture recognition model as a `GestureRecognizer` instance |
| **Foreground Service** | Keeps recognition alive system-wide with a persistent notification |
| **AccessibilityService** | Dispatches `GestureDescription` strokes — no root, no adb required |
| **Quick Settings Tile** | One-tap toggle from the Android notification shade |
| **Jetpack Compose** | Reactive UI reflecting live model output via `SharedFlow` |

---

## ✨ Features

- **🎯 High-Precision Hand Tracking** — MediaPipe localises **21 hand landmarks** in 3D space with sub-30 ms latency on-device.
- **🌐 System-Wide Gesture Injection** — Translates gestures into real `GestureDescription` events (Tap, Double Tap, Swipe Up/Down/Left/Right) that work in every app.
- **🔒 Always-On Background Service** — A Foreground Service with notification keeps gesture detection active regardless of which app is in the foreground.
- **🗺️ Fully Customisable Action Mapping** — Assign any gesture to any system action or app shortcut from within the app's settings UI.
- **📊 Real-Time Test Mode** — An interactive mode overlays skeletal hand tracking and live classification confidence directly on the camera feed.
- **🧩 Custom Gesture & Model Support** — Load your own TFLite `.task` model and label file. Define custom gesture classes trained on your own hand data.
- **📚 Built-in Gesture Library** — Ships with a pre-trained library of common gestures (Thumbs Up, Peace, L-Shape, Open Palm, and more) beyond MediaPipe's defaults.
- **⚡ Quick Settings Tile** — Toggle the recognition service directly from the Android System UI notification shade — no need to open the app.
- **🎨 Modern Compose UI** — Sleek dark-themed interface built end-to-end with **Jetpack Compose**, with smooth transitions and live data observation.

---

## 🖐️ Gesture Mapping

The following gestures are recognised out-of-the-box and can be remapped to any supported system action:

| Gesture | Default Action | Description |
| :--- | :--- | :--- |
| 👍 **Thumbs Up** | Double Tap | Simulates a double tap at the screen centre |
| ☝️ **Pointing Up** | Swipe Up | Executes a vertical swipe from bottom to top |
| 🖐️ **Open Palm** | Custom | Fully configurable in Action Mapping settings |
| 🤟 **ILoveYou** | Custom | Configurable for shortcuts or navigation |
| ✌️ **Peace** | Custom | Extensible via the built-in gesture library |

---

## 🚀 Getting Started

### Prerequisites

| Requirement | Minimum |
|---|---|
| Android OS | 9.0 (API 28) |
| Camera | Front-facing camera |
| Android Studio | Hedgehog (2023.1.1) or later |
| Build Tools | Gradle 8+ / AGP 8+ |

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/KarthikSai06/hand-Gesture-Controller.git
   cd hand-Gesture-Controller
   ```

2. **Add the ML Model**  
   Download the `gesture_recognizer.task` bundle from the [MediaPipe Model Zoo](https://developers.google.com/mediapipe/solutions/vision/gesture_recognizer#models) and place it at:
   ```
   app/src/main/assets/gesture_recognizer.task
   ```

3. **Open & Build**  
   Open the project in Android Studio, let Gradle sync, then run on a physical device.

### Required Permissions & Services

Before the app can function, grant the following on your device:

| Permission / Service | Where to Enable |
|---|---|
| **Camera** | Requested at runtime on first launch |
| **Notifications** | Requested at runtime (Android 13+) |
| **Accessibility Service** | `Settings → Accessibility → GestureControl` |

> **Note:** The Accessibility Service is mandatory. It is the mechanism through which GestureControl injects touch events system-wide without requiring root access.

---

## 🏗️ Technical Architecture Summary

1. **Input Layer (CameraX)** — Captures frames from the front-facing camera and feeds them to the analysis pipeline at a configurable target resolution.
2. **ML Engine (MediaPipe)** — Runs the multi-stage Gesture Recognizer pipeline on-device, producing landmark coordinates and gesture classifications per frame.
3. **Service Layer (Foreground Service)** — Owns the camera and ML engine lifecycle, emitting recognised gesture events onto a `SharedFlow` event bus.
4. **Execution Layer (AccessibilityService)** — Subscribes to the event bus, converts gesture events into `GestureDescription` objects, and dispatches them to Android OS.
5. **Reactive UI (Jetpack Compose)** — Collects from the `SharedFlow` to update the Test Mode overlay and other live UI elements in real time.

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">
  <sub>Built with ❤️ using MediaPipe, CameraX, Jetpack Compose, and Android Accessibility Services.</sub>
</div>
