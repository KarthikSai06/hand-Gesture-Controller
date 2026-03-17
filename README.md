# GestureControl

GestureControl is a sophisticated Android application designed for hands-free device interaction through real-time hand gesture recognition. Utilizing on-device machine learning, the app translates physical gestures into system-wide navigation and touch events.

## Features

- **High-Precision Hand Tracking**: Leverages Google MediaPipe to detect 21 unique hand landmarks with sub-millisecond latency.
- **Universal Gesture Injection**: Seamlessly translates hand movements into system-wide touch events (Tap, Double Tap, Swipes) via Android Accessibility Services.
- **Persistent Background Operation**: A robust Foreground Service ensures gesture detection remains active across all applications and even when the app is minimized.
- **Advanced Action Mapping**: Fully customizable engine to assign specific system actions or navigation shortcuts to recognized gestures.
- **Real-time Visualization**: Interactive "Test Mode" overlaying skeletal hand tracking and classification confidence for precise user feedback.
- **Custom Gesture Framework**: Extensible support for custom TFLite models and a diverse built-in library of common hand gestures.
- **Quick System Integration**: Dedicated Quick Settings Tile for rapid service toggling directly from the Android System UI.
- **Modern Performance**: Sleek, dark-themed interface built entirely with Jetpack Compose for optimal responsiveness.

## Screenshots

| Home & Permissions | Test Mode | Action Mapping | Settings |
| :---: | :---: | :---: | :---: |
| ![Home](https://github.com/KarthikSai06/hand-Gesture-Controller/blob/main/screenshots/home.jpeg?raw=true) | ![Test Mode](https://github.com/KarthikSai06/hand-Gesture-Controller/blob/main/screenshots/test_mode.jpeg?raw=true) | ![Action](https://github.com/KarthikSai06/hand-Gesture-Controller/blob/main/screenshots/action.jpeg?raw=true) | ![Settings](https://github.com/KarthikSai06/hand-Gesture-Controller/blob/main/screenshots/settings.jpeg?raw=true) |

## Gesture Mapping

The application identifies several distinct gestures which can be mapped to system actions:

| Gesture | Default Action | Description |
| :--- | :--- | :--- |
| 👍 **Thumbs Up** | Double Tap | Simulates a double tap at the center of the screen. |
| ☝️ **Pointing Up** | Swipe Up | Executes a vertical swipe from bottom to top. |
| 🖐️ **Open Palm** | Custom | Configurable for various navigation tasks. |
| 🤟 **ILoveYou** | Custom | Configurable for shortcut actions. |

## Technical Architecture

The system follows a modular architecture to ensure performance and reliability:

1.  **Input Layer (CameraX)**: Captures high-frequency frames from the front-facing camera.
2.  **Processing Engine (MediaPipe)**: Analyzes frames on-device to detect 21 hand landmarks and classify gestures.
3.  **Service Layer (Foreground Service)**: Manages the camera lifecycle and coordinates between recognition and execution.
4.  **Execution Layer (Accessibility Service)**: Receives commands from the processing engine and dispatches `GestureDescription` strokes to the Android OS.
5.  **Reactive UI (Jetpack Compose)**: Observes a central `SharedFlow` event bus to provide instantaneous feedback to the user.

## Getting Started

### Prerequisites

- Android 9.0 (API 28) or higher.
- Physical device with a front-facing camera.
- Android Studio Hedgehog (2023.1.1)+.

### Installation

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/KarthikSai06/hand-Gesture-Controller.git
    ```
2.  **Add the ML Model**:
    - Download the `gesture_recognizer.task` file from the [Google MediaPipe Model Zoo](https://developers.google.com/mediapipe/solutions/vision/gesture_recognizer#models).
    - Place the file in `app/src/main/assets/`.
3.  **Build**: Open in Android Studio and sync Gradle.

### Setup

For the application to function, ensure the following are enabled:
- **Camera Permission**: For image analysis.
- **Notification Permission**: To keep the service active in the background.
- **Accessibility Service**: Enable `GestureControl` in `System Settings > Accessibility`.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
