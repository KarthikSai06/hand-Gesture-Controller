# GestureControl

GestureControl is a sophisticated Android application designed for hands-free device interaction through real-time hand gesture recognition. Utilizing on-device machine learning, the app translates physical gestures into system-wide navigation and touch events.

## Features

- **Low-Latency Detection**: Powered by Google MediaPipe for responsive and accurate gesture tracking.
- **Background Processing**: Runs as a foreground service to provide continuous detection across all applications.
- **System Integration**: Injects touch events (taps, swipes) via Android's Accessibility Service.
- **Visual Debugging**: Includes a "Test Mode" to visualize hand landmarks and detection confidence in real-time.
- **Flexible Mapping**: Assign custom system actions to various hand gestures.
- **User-Centric Design**: Modern, dark-themed interface built entirely with Jetpack Compose.

## Screenshots

| Home & Permissions | Test Mode | Action Mapping | Settings |
| :---: | :---: | :---: | :---: |
| <img src="screenshots/home.png" width="200" /> | <img src="screenshots/test_mode.png" width="200" /> | <img src="screenshots/actions.png" width="200" /> | <img src="screenshots/settings.png" width="200" /> |

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
    git clone https://github.com/YOUR_USERNAME/GestureControl.git
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
