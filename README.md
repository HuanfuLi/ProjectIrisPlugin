# Lumi Agent - Next-Generation AI Assistant for XR
-----

**Lumi Agent** is a multimodal AI assistant designed specifically for Extended Reality (XR) environments. Built for the **Meta Quest 3**, it provides a hands-free, always-on, and context-aware experience that seamlessly integrates into the user's physical space.

Unlike traditional AI interactions that require manual input or app switching, Lumi utilizes the **Gemini 2.0 Flash API** and a custom **Android-Unity bridge** to analyze audio and video streams in real-time. It is activated by a simple "Snap" gesture, allowing users to interact with the AI without obstructing their view or breaking immersion.

**Note**: This project is not open-sourced. This repository only contains code in the Android plugin. Front-end C# code is not yet disclosed. To preview the complete project, please watch the demo video below:

### 🎥 Project Demo

[**Watch the Demo Video**](https://drive.google.com/file/d/1S729FcmwS1X4VORojB_WLLj_0Qs0wadJ/view?usp=sharing)

-----

## Key Features

  * **Spatial UI**: A dynamic, non-obtrusive user interface that lives in 3D space and updates based on the user's field of view.
  * **Multimodal Interaction**: Capable of processing both voice commands and live visual context (video streaming) simultaneously.
  * **Hands-Free Activation**: Triggered by a custom "Snap" gesture, eliminating the need for wake words or controller inputs.
  * **Real-Time Context**: Streams visual data from the Quest 3 passthrough camera to Google Gemini Cloud AI for instant environmental understanding.
  * **Low Latency**: Achieves an average response time of **0.92 seconds**—significantly faster than standard mobile AI interactions.

-----

## System Architecture

The project utilizes a split architecture to leverage the best of XR rendering and native hardware performance.

### Front-end (Unity & C\#)

  * **Engine**: Unity (Core engine for Spatial UI).
  * **Logic**: C\# scripts handle interaction logic, UI management, and gesture detection.
  * **Snap Gesture**: A velocity-based detection algorithm (`SnapGestureDetector.cs`) tracks the distance and speed between the thumb and middle finger to trigger the AI.

### Back-end (Android Native & Java)

  * **Hardware Bridge**: A custom Android plugin (`IrisPlugin.java`) accesses low-level hardware features that Unity cannot reach directly, such as the Quest's passthrough camera and raw microphone data.
  * **AI Engine**: **Gemini 2.0 Flash API** serves as the brain of the agent.
  * **Communication**: Uses **WebSockets** for bidirectional, real-time streaming of PCM 16-bit audio and JPEG images.
  * **Audio Playback**: Decodes and plays back AI audio chunks immediately via an AudioTrack buffer to ensure low latency.

-----

## Performance

Based on internal testing, Lumi Agent outperforms standard AI interaction methods in speed and convenience:

  * **Average Time-to-Speak**: 0.92 seconds.
  * **Comparison**: \~82% faster than the Google Gemini App workflow and \~49% faster than current Meta AI Glasses Time-to-Speak latency.
  * **Gesture Accuracy**: The refined "Snap" gesture algorithm maintains an 82% success rate while significantly reducing accidental triggers common with standard pinch gestures.

-----

## File Structure

```text
UnityPlugin/
|── app/
|   |── java/com.projectiris.unityplugin/
|       |── AiInteration.java       # Core WebSocket & Gemini logic
|       |── IrisPlugin.java         # JNI Entry point
|       |── QuestCameraHelper.java  # Passthrough camera access
|   |── res/
|── build.gradle
```

-----

## Usage

1.  **Launch**: Open the Lumi Agent app on your Meta Quest 3.
2.  **Activate**: Perform a "Snap" gesture (rapidly snapping thumb and middle finger) with either hand.
3.  **Interact**: Speak naturally. The agent will analyze your voice and your current view (what you are looking at).
4.  **Response**: The AI's response will be displayed on the spatial UI floating in front of you and spoken through the headset speakers.

-----

## Future Work

  * **Real-time 3D Marking**: Implementation of spatial markers to highlight real-world objects using the Meta Depth API.
  * **External Integrations**: Connecting Lumi with Google Workspace (Calendar, Gmail) via Model Control Protocols (MCP).
  * **Local Speech Detection**: Implementing local Wake-Word detection (e.g., Whisper) to replace or augment the gesture trigger.

-----

## Contributors

  * **Huanfu Li**: Core integration (Gemini API, Android Library), Unity UI/UX, C\# Logic, JNI, and Web Portal.
  * **[Jingcao Hu](https://github.com/JingcaoHu)**: Version control, Android testing/refactoring, and JSON communication implementation.

## Acknowledgments
* **Meta Developers & Google Gemini API Team**: For providing the SDKs and documentation necessary for this project.
* **[yeyu2](https://github.com/yeyu2)**: Special thanks for the code tutorials in the [Youtube_demos](https://github.com/yeyu2/Youtube_demos/tree/main/gemini20-android-serverless/GeminiLiveDemo) repository, which provided guidance on serverless Gemini Live API integration on an Android device.
