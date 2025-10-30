# Unity Meta Quest 3 AI Integration Guide

## Overview

This guide explains how to integrate the `IrisPlugin.aar` Android library into your Unity Meta Quest 3 project for AI interaction with camera and microphone capabilities.

## 🚀 Quick Setup

### 1. Copy AAR File to Unity Project

```
YourUnityProject/
├── Assets/
│   └── Plugins/
│       └── Android/
│           ├── IrisPlugin.aar          # Copy from app/build/outputs/aar/app-release.aar
│           └── AndroidManifest.xml     # Optional: Custom manifest
```

### 2. Unity C# Integration Script

Create a script called `IrisAIManager.cs`:

```csharp
using UnityEngine;
using System;

public class IrisAIManager : MonoBehaviour
{
    [Header("AI Configuration")]
    public string geminiApiKey = "YOUR_GEMINI_API_KEY";
    public bool enableDebugLogs = true;
    
    private AndroidJavaObject irisPlugin;
    private AndroidJavaObject unityActivity;
    
    // Events for AI responses
    public static event Action<string, string> OnMessageReceived;
    public static event Action<string> OnApiResult;
    public static event Action<bool> OnConnectionStatusChanged;
    
    private void Start()
    {
        InitializePlugin();
        RequestPermissions();
    }
    
    private void InitializePlugin()
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        try
        {
            // Get Unity Activity
            var unityClass = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            unityActivity = unityClass.GetStatic<AndroidJavaObject>("currentActivity");
            
            // Initialize IrisPlugin
            var pluginClass = new AndroidJavaClass("com.projectiris.unityplugin.IrisPlugin");
            pluginClass.CallStatic("initialize", unityActivity);
            
            Log("IrisPlugin initialized successfully");
        }
        catch (Exception e)
        {
            LogError($"Failed to initialize IrisPlugin: {e.Message}");
        }
        #endif
    }
    
    private void RequestPermissions()
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        // Request camera permission for Quest 3 Passthrough API
        if (!Permission.HasUserAuthorizedPermission("android.permission.CAMERA"))
        {
            Permission.RequestUserPermission("android.permission.CAMERA");
        }
        
        // Request microphone permission
        if (!Permission.HasUserAuthorizedPermission(Permission.Microphone))
        {
            Permission.RequestUserPermission(Permission.Microphone);
        }
        
        // Request Quest-specific headset camera permission
        if (!Permission.HasUserAuthorizedPermission("horizonos.permission.HEADSET_CAMERA"))
        {
            Permission.RequestUserPermission("horizonos.permission.HEADSET_CAMERA");
        }
        #endif
    }
    
    // Main API methods
    public void StartAIWithApiKey(string apiKey)
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        try
        {
            var pluginClass = new AndroidJavaClass("com.projectiris.unityplugin.IrisPlugin");
            pluginClass.CallStatic("performActionWithApiKey", apiKey);
            Log($"AI started with API key: {apiKey.Substring(0, 10)}...");
        }
        catch (Exception e)
        {
            LogError($"Failed to start AI: {e.Message}");
        }
        #endif
    }
    
    public void StartConnection()
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        var pluginClass = new AndroidJavaClass("com.projectiris.unityplugin.IrisPlugin");
        pluginClass.CallStatic("startConnection");
        Log("AI connection started");
        #endif
    }
    
    public void StopConnection()
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        var pluginClass = new AndroidJavaClass("com.projectiris.unityplugin.IrisPlugin");
        pluginClass.CallStatic("stopConnection");
        Log("AI connection stopped");
        #endif
    }
    
    public void StartCamera()
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        var pluginClass = new AndroidJavaClass("com.projectiris.unityplugin.IrisPlugin");
        pluginClass.CallStatic("startCamera");
        Log("Camera started");
        #endif
    }
    
    public void StopCamera()
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        var pluginClass = new AndroidJavaClass("com.projectiris.unityplugin.IrisPlugin");
        pluginClass.CallStatic("stopCamera");
        Log("Camera stopped");
        #endif
    }
    
    public void StartRecording()
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        var pluginClass = new AndroidJavaClass("com.projectiris.unityplugin.IrisPlugin");
        pluginClass.CallStatic("startRecording");
        Log("Audio recording started");
        #endif
    }
    
    public void StopRecording()
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        var pluginClass = new AndroidJavaClass("com.projectiris.unityplugin.IrisPlugin");
        pluginClass.CallStatic("stopRecording");
        Log("Audio recording stopped");
        #endif
    }
    
    // Status check methods
    public bool IsConnected()
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        var pluginClass = new AndroidJavaClass("com.projectiris.unityplugin.IrisPlugin");
        return pluginClass.CallStatic<bool>("isConnected");
        #else
        return false;
        #endif
    }
    
    public bool IsRecording()
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        var pluginClass = new AndroidJavaClass("com.projectiris.unityplugin.IrisPlugin");
        return pluginClass.CallStatic<bool>("isRecording");
        #else
        return false;
        #endif
    }
    
    public bool IsCameraActive()
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        var pluginClass = new AndroidJavaClass("com.projectiris.unityplugin.IrisPlugin");
        return pluginClass.CallStatic<bool>("isCameraActive");
        #else
        return false;
        #endif
    }
    
    public void SetMuted(bool muted)
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        var pluginClass = new AndroidJavaClass("com.projectiris.unityplugin.IrisPlugin");
        pluginClass.CallStatic("setMuted", muted);
        Log($"Muted mode set to: {muted}");
        #endif
    }
    
    public void SendImageToAI(byte[] imageData)
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        var pluginClass = new AndroidJavaClass("com.projectiris.unityplugin.IrisPlugin");
        pluginClass.CallStatic("sendImage", imageData);
        Log($"Image sent to AI ({imageData.Length} bytes)");
        #endif
    }
    
    // Unity callback methods (called from Android plugin)
    public void OnApiResultReceived(string result)
    {
        Log($"API Result: {result}");
        OnApiResult?.Invoke(result);
    }
    
    public void OnMessageReceived(string jsonMessage)
    {
        try
        {
            // Parse JSON message from plugin
            var messageData = JsonUtility.FromJson<AIMessage>(jsonMessage);
            Log($"AI Message [{messageData.type}]: {messageData.message}");
            OnMessageReceived?.Invoke(messageData.message, messageData.type);
        }
        catch (Exception e)
        {
            LogError($"Failed to parse AI message: {e.Message}");
        }
    }
    
    private void OnDestroy()
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        try
        {
            var pluginClass = new AndroidJavaClass("com.projectiris.unityplugin.IrisPlugin");
            pluginClass.CallStatic("cleanUp");
            Log("IrisPlugin cleaned up");
        }
        catch (Exception e)
        {
            LogError($"Error during cleanup: {e.Message}");
        }
        #endif
    }
    
    private void Log(string message)
    {
        if (enableDebugLogs)
            Debug.Log($"[IrisAI] {message}");
    }
    
    private void LogError(string message)
    {
        Debug.LogError($"[IrisAI] {message}");
    }
}

[Serializable]
public class AIMessage
{
    public string type;
    public string message;
}
```

### 3. AI Interaction Controller

Create `AIInteractionController.cs` for easy management:

```csharp
using UnityEngine;
using UnityEngine.UI;
using TMPro;

public class AIInteractionController : MonoBehaviour
{
    [Header("UI References")]
    public Button startConnectionButton;
    public Button stopConnectionButton;
    public Button startCameraButton;
    public Button stopCameraButton;
    public Button startRecordingButton;
    public Button stopRecordingButton;
    public TextMeshProUGUI statusText;
    public TextMeshProUGUI messageLog;
    public Toggle mutedToggle;
    
    [Header("Configuration")]
    public string geminiApiKey = "YOUR_GEMINI_API_KEY_HERE";
    
    private IrisAIManager aiManager;
    private string logText = "";
    
    private void Start()
    {
        // Get AI Manager
        aiManager = FindObjectOfType<IrisAIManager>();
        if (aiManager == null)
        {
            GameObject go = new GameObject("IrisAIManager");
            aiManager = go.AddComponent<IrisAIManager>();
        }
        
        // Setup UI events
        SetupUIEvents();
        
        // Subscribe to AI events
        IrisAIManager.OnMessageReceived += OnAIMessageReceived;
        IrisAIManager.OnApiResult += OnApiResult;
        
        // Start with API key
        aiManager.StartAIWithApiKey(geminiApiKey);
        
        // Start update loop for status
        InvokeRepeating(nameof(UpdateStatus), 1f, 1f);
    }
    
    private void SetupUIEvents()
    {
        startConnectionButton?.onClick.AddListener(() => aiManager.StartConnection());
        stopConnectionButton?.onClick.AddListener(() => aiManager.StopConnection());
        startCameraButton?.onClick.AddListener(() => aiManager.StartCamera());
        stopCameraButton?.onClick.AddListener(() => aiManager.StopCamera());
        startRecordingButton?.onClick.AddListener(() => aiManager.StartRecording());
        stopRecordingButton?.onClick.AddListener(() => aiManager.StopRecording());
        mutedToggle?.onValueChanged.AddListener((bool muted) => aiManager.SetMuted(muted));
    }
    
    private void UpdateStatus()
    {
        if (statusText != null)
        {
            string status = $"Connected: {aiManager.IsConnected()}\\n";
            status += $"Recording: {aiManager.IsRecording()}\\n";
            status += $"Camera: {aiManager.IsCameraActive()}";
            statusText.text = status;
        }
    }
    
    private void OnAIMessageReceived(string message, string type)
    {
        AddToLog($"[{type.ToUpper()}] {message}");
    }
    
    private void OnApiResult(string result)
    {
        AddToLog($"[API] {result}");
    }
    
    private void AddToLog(string message)
    {
        logText += $"{System.DateTime.Now:HH:mm:ss} {message}\\n";
        
        // Keep only last 20 lines
        string[] lines = logText.Split('\\n');
        if (lines.Length > 20)
        {
            logText = string.Join("\\n", lines, lines.Length - 20, 20);
        }
        
        if (messageLog != null)
            messageLog.text = logText;
    }
    
    private void OnDestroy()
    {
        IrisAIManager.OnMessageReceived -= OnAIMessageReceived;
        IrisAIManager.OnApiResult -= OnApiResult;
    }
}
```

## 📱 Meta Quest 3 Specific Setup

### 1. Player Settings Configuration

In Unity Player Settings → Android:

```
- Minimum API Level: 29 (Android 10.0)
- Target API Level: 34 (Android 14.0)
- Scripting Backend: IL2CPP
- Target Architectures: ARM64
- Install Location: Auto
```

### 2. XR Plugin Management

Install and configure:
- **Oculus XR Plugin** (Meta XR SDK)
- **XR Interaction Toolkit**

### 3. Required Permissions in AndroidManifest.xml

Create `Assets/Plugins/Android/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Meta Quest 3 Compatible Permissions -->
    <!-- Camera access via Passthrough Camera API -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="horizonos.permission.HEADSET_CAMERA" />
    
    <!-- Microphone access -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    
    <!-- Network permissions for AI communication -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- VR Hardware Features for Meta Quest 3 -->
    <uses-feature android:name="android.hardware.vr.headtracking" android:required="true" android:version="1" />
    <uses-feature android:name="android.software.vr.mode" android:required="false" />
    
    <!-- Hardware Features -->
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-feature android:name="android.hardware.microphone" android:required="false" />

    <application android:theme="@android:style/Theme.Black.NoTitleBar.Fullscreen">
        <activity android:name="com.unity3d.player.UnityPlayerActivity"
                  android:exported="true"
                  android:launchMode="singleTask"
                  android:screenOrientation="landscape"
                  android:configChanges="mcc|mnc|locale|touchscreen|keyboard|keyboardHidden|navigation|orientation|screenLayout|uiMode|screenSize|smallestScreenSize|fontScale|layoutDirection|density">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="com.oculus.intent.category.VR" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

## 🎮 Usage Examples

### Basic AI Interaction

```csharp
public class BasicAIExample : MonoBehaviour
{
    private IrisAIManager aiManager;
    
    private void Start()
    {
        aiManager = FindObjectOfType<IrisAIManager>();
        
        // Subscribe to AI responses
        IrisAIManager.OnMessageReceived += HandleAIMessage;
        
        // Start AI with your API key
        aiManager.StartAIWithApiKey("YOUR_GEMINI_API_KEY");
        
        // Start camera and recording
        aiManager.StartConnection();
        aiManager.StartCamera();
        aiManager.StartRecording();
    }
    
    private void HandleAIMessage(string message, string type)
    {
        switch (type)
        {
            case "transcript":
                Debug.Log($"AI Transcript: {message}");
                break;
            case "text":
                Debug.Log($"AI Response: {message}");
                break;
        }
    }
}
```

### Advanced Camera Integration

```csharp
public class CameraAIIntegration : MonoBehaviour
{
    public Camera vrCamera;
    public RenderTexture renderTexture;
    
    private IrisAIManager aiManager;
    
    private void Start()
    {
        aiManager = FindObjectOfType<IrisAIManager>();
        
        // Start camera capture
        aiManager.StartCamera();
        
        // Optionally send manual screenshots
        InvokeRepeating(nameof(SendScreenshot), 2f, 5f);
    }
    
    private void SendScreenshot()
    {
        if (aiManager.IsCameraActive())
        {
            // Capture screen and send to AI
            byte[] imageData = CaptureScreenshot();
            aiManager.SendImageToAI(imageData);
        }
    }
    
    private byte[] CaptureScreenshot()
    {
        // Implementation for capturing screenshot
        // This is optional since the plugin handles camera automatically
        RenderTexture.active = renderTexture;
        vrCamera.Render();
        
        Texture2D screenshot = new Texture2D(renderTexture.width, renderTexture.height, TextureFormat.RGB24, false);
        screenshot.ReadPixels(new Rect(0, 0, renderTexture.width, renderTexture.height), 0, 0);
        screenshot.Apply();
        
        byte[] data = screenshot.EncodeToJPG(70);
        DestroyImmediate(screenshot);
        
        return data;
    }
}
```

## 🔧 Build Configuration

### 1. Build Settings

```
Platform: Android
Architecture: ARM64
Development Build: ✓ (for debugging)
Script Debugging: ✓ (for debugging)
```

### 2. Build and Deploy

1. **Build APK/AAB**: Build → Build Settings → Build
2. **Install on Quest 3**: Use Meta Quest Developer Hub or ADB
3. **Enable Developer Mode** on Quest 3
4. **Grant Permissions**: Camera, Microphone, and Headset Camera

## 🐛 Troubleshooting

### Common Issues

1. **Camera Not Working**
   - Ensure Quest 3 has Horizon OS v74+ 
   - Check `horizonos.permission.HEADSET_CAMERA` permission
   - Verify Passthrough is enabled in Quest settings

2. **Microphone Not Recording**
   - Check `RECORD_AUDIO` permission
   - Ensure microphone access is granted in Quest settings
   - Test with Unity's built-in microphone first

3. **Network Connection Issues**
   - Verify internet connection on Quest 3
   - Check Gemini API key validity
   - Enable internet permission in manifest

4. **Plugin Not Found**
   - Ensure AAR file is in `Assets/Plugins/Android/`
   - Check if plugin class path is correct
   - Verify Android build settings

### Debug Commands

```csharp
// Check plugin status
Debug.Log($"Connected: {aiManager.IsConnected()}");
Debug.Log($"Recording: {aiManager.IsRecording()}");
Debug.Log($"Camera Active: {aiManager.IsCameraActive()}");

// Test permissions
Debug.Log($"Camera Permission: {Permission.HasUserAuthorizedPermission("android.permission.CAMERA")}");
Debug.Log($"Microphone Permission: {Permission.HasUserAuthorizedPermission(Permission.Microphone)}");
```

## 📚 API Reference

### IrisPlugin Static Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `initialize(Activity)` | Initialize plugin with Unity activity | void |
| `performActionWithApiKey(String)` | Start AI with API key | void |
| `startConnection()` | Start WebSocket connection | void |
| `stopConnection()` | Stop WebSocket connection | void |
| `startCamera()` | Start camera capture | void |
| `stopCamera()` | Stop camera capture | void |
| `startRecording()` | Start audio recording | void |
| `stopRecording()` | Stop audio recording | void |
| `isConnected()` | Check connection status | boolean |
| `isRecording()` | Check recording status | boolean |
| `isCameraActive()` | Check camera status | boolean |
| `setMuted(boolean)` | Set muted mode | void |
| `isMuted()` | Get muted status | boolean |
| `sendImage(byte[])` | Send image to AI | void |
| `cleanUp()` | Clean up resources | void |

### Unity Callbacks

The plugin calls these Unity methods:

- `AndroidPluginManager.OnApiResultReceived(string)` - API operation results
- `AndroidPluginManager.OnMessageReceived(string)` - AI messages in JSON format

## 🚀 Performance Tips

1. **Camera Optimization**: The plugin automatically handles camera capture at 30 FPS with optimized resolution
2. **Memory Management**: Always call `cleanUp()` when done
3. **Threading**: All heavy operations run on background threads
4. **Battery Optimization**: Stop camera/recording when not needed

## 📝 Notes

- **Camera Access**: Uses Meta's Passthrough Camera API (available since 2024)
- **Microphone Access**: Standard Android audio recording API
- **Network**: Requires stable internet for AI communication
- **Permissions**: Must be granted before using camera/microphone features
- **Quest Store**: Compatible with Meta Horizon Store requirements

This integration provides full AI interaction capabilities on Meta Quest 3 with camera and microphone input for immersive VR experiences.