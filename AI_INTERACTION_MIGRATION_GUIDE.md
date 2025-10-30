# AiInteraction.java Migration and Usage Guide

## Overview

The `AiInteration.java` class has been successfully refactored into a standalone, object-oriented AI interaction library that can be easily integrated into Android applications or migrated to Unity plugins. This document provides comprehensive guidance on using the refactored AI interaction system.

## 🚀 Quick Start

### Basic Integration

```java
// 1. Initialize AiInteraction
AiInteration aiInteraction = new AiInteration("YOUR_API_KEY", false);

// 2. Set up message callbacks for UI updates
aiInteraction.setMessageCallback((message, type) -> {
    // Handle received messages in your UI
    displayMessage(message);
});

// 3. Start the connection
aiInteraction.start();

// 4. Start camera capture (requires CAMERA permission)
aiInteraction.startCamera(context);

// 5. Start recording audio (requires RECORD_AUDIO permission)
aiInteraction.startRecording();

// 6. Stop recording and camera
aiInteraction.stopRecording();
aiInteraction.stopCamera();

// 7. Clean up when done
aiInteraction.cleanUp();
```

## 📋 Complete API Reference

### Constructor

```java
AiInteration(String API_KEY, boolean muted)
```

- **API_KEY**: Your Google Gemini API key
- **muted**: If `true`, only text responses; if `false`, audio + text responses

### Core Methods

#### Connection Management
```java
void start()                    // Start WebSocket connection
void stop()                     // Stop WebSocket connection
boolean isConnected()           // Check connection status
```

#### Audio Recording
```java
@RequiresPermission(Manifest.permission.RECORD_AUDIO)
void startRecording()           // Start audio recording
void stopRecording()            // Stop audio recording
boolean isRecording()           // Check recording status
```

#### Camera Capture (No Preview UI)
```java
@RequiresPermission(Manifest.permission.CAMERA)
void startCamera(Context context)   // Start camera capture without preview
void stopCamera()                   // Stop camera capture
boolean isCameraActive()            // Check camera status
```

#### Image Processing
```java
void sendImage(byte[] imageBytes)  // Send image data to AI
```

#### Callback Management
```java
void setMessageCallback(MessageCallback callback)  // Set message callback
```

#### Resource Management
```java
void cleanUp()                  // Clean up all resources
```

### MessageCallback Interface

```java
public interface MessageCallback {
    void onMessageReceived(String message, String type);
}
```

**Message Types:**
- `"transcript"`: Audio transcription from AI
- `"text"`: Direct text response from AI

## 🏗️ Architecture Overview

### Class Hierarchy
```
AiInteration
├── WebSocket Management
├── Audio Recording/Playback
├── Image Processing
├── Message Parsing
└── Resource Management
```

### Key Features
- **Encapsulated AI Logic**: All AI interaction logic is contained within the class
- **Complete Camera Integration**: Camera capture without preview UI, sends images to AI automatically
- **Fixed Audio Playback**: Restored working audio implementation from MainActivityLegacy.java
- **Thread-Safe Operations**: Uses synchronized collections and proper threading
- **Automatic Resource Management**: Handles cleanup of audio, WebSocket, and camera resources
- **Callback-Based UI Updates**: Non-blocking message delivery to UI components
- **Permission Handling**: Proper Android permission management

## 📱 Android Integration Example

### MainActivity Implementation

```java
public class MainActivity extends AppCompatActivity {
    private AiInteration aiInteraction;
    private TextView chatLog;
    private Button startButton, stopButton;
    private ImageView statusIndicator;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        setupAiInteraction();
        setupListeners();
    }
    
    private void setupAiInteraction() {
        aiInteraction = new AiInteration("YOUR_API_KEY", false);
        
        // Set up message callback
        aiInteraction.setMessageCallback((message, type) -> {
            runOnUiThread(() -> displayMessage(message));
        });
        
        // Start connection
        aiInteraction.start();
        
        // Start camera capture (no preview)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            aiInteraction.startCamera(this);
        }
    }
    
    private void setupListeners() {
        // Camera capture button
        captureButton.setOnClickListener(v -> {
            if (aiInteraction.isCameraActive()) {
                aiInteraction.stopCamera();
                captureButton.setText("Start Capture");
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    aiInteraction.startCamera(this);
                    captureButton.setText("Stop Capture");
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });
        
        // Audio recording buttons
        startButton.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                aiInteraction.startRecording();
                updateUI();
            } else {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 200);
            }
        });
        
        stopButton.setOnClickListener(v -> {
            aiInteraction.stopRecording();
            updateUI();
        });
    }
    
    private void updateUI() {
        runOnUiThread(() -> {
            if (!aiInteraction.isConnected()) {
                statusIndicator.setColorFilter(Color.RED);
            } else if (aiInteraction.isRecording()) {
                statusIndicator.setColorFilter(Color.GREEN);
            } else {
                statusIndicator.setColorFilter(Color.GRAY);
            }
            
            // Update camera button text
            if (aiInteraction.isCameraActive()) {
                captureButton.setText("Stop Capture");
            } else {
                captureButton.setText("Start Capture");
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiInteraction != null) {
            aiInteraction.cleanUp();
        }
    }
}
```

## 🎮 Unity Plugin Migration

### C# Wrapper Interface

```csharp
public class AiInteractionPlugin : MonoBehaviour 
{
    private AndroidJavaObject aiInteraction;
    private AndroidJavaClass unityPlayer;
    
    void Start() 
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
        AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
        
        // Initialize AiInteraction
        aiInteraction = new AndroidJavaObject("com.projectiris.geminiliveapitest.AiInteration", 
                                               "YOUR_API_KEY", false);
        
        // Set up callback bridge
        SetupCallbackBridge();
        
        // Start connection
        aiInteraction.Call("start");
        #endif
    }
    
    public void StartCamera() 
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
        aiInteraction.Call("startCamera", currentActivity);
        #endif
    }
    
    public void StopCamera() 
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        aiInteraction.Call("stopCamera");
        #endif
    }
    
    public void StartRecording() 
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        aiInteraction.Call("startRecording");
        #endif
    }
    
    public void StopRecording() 
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        aiInteraction.Call("stopRecording");
        #endif
    }
    
    public bool IsCameraActive() 
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        return aiInteraction.Call<bool>("isCameraActive");
        #else
        return false;
        #endif
    }
    
    public bool IsConnected() 
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        return aiInteraction.Call<bool>("isConnected");
        #else
        return false;
        #endif
    }
    
    void OnDestroy() 
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        if (aiInteraction != null) {
            aiInteraction.Call("cleanUp");
        }
        #endif
    }
}
```

### Unity Plugin Structure
```
UnityProject/
├── Plugins/
│   └── Android/
│       ├── AiInteration.java          # Complete AI interaction class
│       └── AndroidManifest.xml        # Camera + Audio permissions
├── Scripts/
│   └── AiInteractionPlugin.cs         # Unity C# wrapper
└── Scenes/
    └── MainScene.unity                # Game scene with AI integration
```

## 🔧 Configuration Options

### Muted Mode Configuration
```java
// Text-only mode (no audio)
AiInteration aiInteraction = new AiInteration("API_KEY", true);

// Audio + Text mode
AiInteration aiInteraction = new AiInteration("API_KEY", false);
```

### Custom Message Handling
```java
aiInteraction.setMessageCallback((message, type) -> {
    switch (type) {
        case "transcript":
            handleTranscript(message);
            break;
        case "text":
            handleTextResponse(message);
            break;
        default:
            handleGenericMessage(message);
    }
});
```

## 🛡️ Security Best Practices

### API Key Management
```java
// ❌ Don't hardcode API keys
private static final String API_KEY = "AIzaSyD...";

// ✅ Use secure storage or build configuration
String apiKey = BuildConfig.GEMINI_API_KEY;
// or
String apiKey = getSharedPreferences("secure", MODE_PRIVATE)
                    .getString("api_key", "");
```

### Permission Handling
```java
private void checkPermissions() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
        != PackageManager.PERMISSION_GRANTED) {
        
        ActivityCompat.requestPermissions(this, 
            new String[]{Manifest.permission.RECORD_AUDIO}, 
            AUDIO_REQUEST_CODE);
    }
}
```

## 🚨 Error Handling

### Connection Error Handling
```java
// Monitor connection status
aiInteraction.setMessageCallback((message, type) -> {
    if (!aiInteraction.isConnected()) {
        // Handle disconnection
        showConnectionError();
        attemptReconnection();
    }
});

private void attemptReconnection() {
    new Handler().postDelayed(() -> {
        if (!aiInteraction.isConnected()) {
            aiInteraction.start();
        }
    }, 5000); // Retry after 5 seconds
}
```

### Audio Permission Errors
```java
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                     @NonNull int[] grantResults) {
    if (requestCode == AUDIO_REQUEST_CODE) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                aiInteraction.startRecording();
            } catch (SecurityException e) {
                Log.e(TAG, "Permission error: " + e.getMessage());
                showPermissionError();
            }
        } else {
            showPermissionDeniedMessage();
        }
    }
}
```

## 📊 Performance Considerations

### Memory Management
- The `cleanUp()` method properly releases all resources
- Audio buffers are managed with synchronized collections
- Camera resources are automatically closed
- WebSocket connections are properly terminated

### Threading
- Audio processing runs on background threads
- UI updates are dispatched to the main thread via callbacks
- WebSocket operations are handled asynchronously

### Battery Optimization
```java
// Stop unnecessary services when app goes to background
@Override
protected void onPause() {
    super.onPause();
    if (aiInteraction != null && aiInteraction.isRecording()) {
        aiInteraction.stopRecording();
    }
}

@Override
protected void onResume() {
    super.onResume();
    // Reconnect if needed
    if (aiInteraction != null && !aiInteraction.isConnected()) {
        aiInteraction.start();
    }
}
```

## 🔄 Migration Checklist

### From MainActivityLegacy.java to AiInteration.java

- [✅] **Remove duplicate WebSocket code** from MainActivity
- [✅] **Remove duplicate audio handling** from MainActivity
- [✅] **Remove duplicate message parsing** from MainActivity
- [✅] **Remove camera preview UI** (TextureView removed from layout)
- [✅] **Integrate camera capture logic** into AiInteration.java without preview
- [✅] **Fix audio playback** using working MainActivityLegacy.java implementation
- [✅] **Replace direct AI calls** with AiInteration method calls
- [✅] **Implement MessageCallback** for UI updates
- [✅] **Update permission handling** to delegate to AiInteration
- [✅] **Update lifecycle management** to use AiInteration.cleanUp()
- [✅] **Test all functionality** matches original (with camera capture, no preview)

### Code Size Reduction
- **Before**: MainActivity.java ~800+ lines (MainActivityLegacy.java)
- **After**: MainActivity.java ~250 lines (clean, focused UI logic)
- **Reduction**: ~69% code reduction in MainActivity
- **AiInteration.java**: ~710 lines (complete AI interaction encapsulation)
- **Benefits**: 
  - Better separation of concerns
  - Easier testing and maintenance
  - Reusable AI logic for Unity plugins
  - Camera capture without UI preview
  - Fixed audio playback implementation

## 🧪 Testing Guide

### Unit Testing AiInteration
```java
@Test
public void testAiInteractionInitialization() {
    AiInteration ai = new AiInteration("test_key", true);
    assertFalse(ai.isConnected());
    assertFalse(ai.isRecording());
}

@Test
public void testMessageCallback() {
    AiInteration ai = new AiInteration("test_key", true);
    List<String> receivedMessages = new ArrayList<>();
    
    ai.setMessageCallback((message, type) -> {
        receivedMessages.add(message);
    });
    
    // Simulate message reception
    // Assert callback was triggered
}
```

### Integration Testing
```java
@Test
public void testMainActivityIntegration() {
    // Test that MainActivity properly delegates to AiInteration
    // Test UI updates via callbacks
    // Test permission handling
    // Test lifecycle management
}
```

## 📚 Additional Resources

## 📦 Dependencies and Configuration

### Android Manifest Permissions
```xml
<!-- AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Required Permissions -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Hardware Features -->
    <uses-feature android:name="android.hardware.camera2.full" android:required="false" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-feature android:name="android.hardware.microphone" android:required="false" />

    <application
        android:usesCleartextTraffic="true"
        android:theme="@style/Theme.AppName">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### Gradle Dependencies (build.gradle.kts)
```kotlin
android {
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.yourcompany.yourapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    // Core Android Dependencies
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    
    // WebSocket Communication (REQUIRED)
    implementation("org.java-websocket:Java-WebSocket:1.5.3")
    
    // HTTP Client (Optional - for enhanced networking)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON Processing (Optional - if using Gson instead of built-in JSONObject)
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Google Auth Library (Optional - for Vertex AI authentication)
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
```

### Minimum Requirements
- **Android API Level**: 26 (Android 8.0) or higher
- **Java Version**: 11
- **Kotlin Version**: 2.2.21
- **Android Gradle Plugin**: 8.12.1
- **Target SDK**: 34 (Android 14)

### Essential Dependencies
| Dependency | Version | Purpose | Required |
|------------|---------|---------|----------|
| `Java-WebSocket` | 1.5.3 | WebSocket communication with Gemini API | ✅ **Required** |
| `androidx.appcompat` | 1.7.1 | Android compatibility library | ✅ **Required** |
| `material` | 1.13.0 | Material Design components | ✅ **Required** |
| `constraintlayout` | 2.2.1 | Layout management | ✅ **Required** |
| `okhttp3` | 4.12.0 | Enhanced HTTP client | ⚠️ Optional |
| `gson` | 2.10.1 | JSON processing alternative | ⚠️ Optional |
| `google-auth-library` | 1.19.0 | Authentication for Vertex AI | ⚠️ Optional |

### Logging Configuration
```java
// Enable detailed logging for debugging
private static final String TAG = "AiInteration";
private static final String textTAG = "textOutput";

// Log levels:
// Log.d() - Debug messages
// Log.i() - Info messages  
// Log.w() - Warning messages
// Log.e() - Error messages
```

## 🎯 Next Steps

1. **Test the refactored implementation** with your specific use case
2. **Customize the MessageCallback** for your UI requirements
3. **Implement proper error handling** for production use
4. **Consider adding more callback types** for different events
5. **Optimize for your specific Android/Unity requirements**
6. **Add unit tests** for critical functionality
7. **Implement proper logging** and analytics

## 🔧 Latest Implementation Details

### Audio Playback Fix
The audio implementation now uses the exact same working logic from MainActivityLegacy.java:
```java
private AudioTrack audioTrack;  // Single persistent instance

private void playAudio(byte[] byteArray) {
    if (audioTrack == null) {
        audioTrack = new AudioTrack(...);
    }
    audioTrack.write(byteArray, 0, byteArray.length);
    audioTrack.play();
    // Simple background monitoring for completion
}
```

### Camera Implementation Without Preview
- Uses `CameraDevice.TEMPLATE_STILL_CAPTURE` instead of `TEMPLATE_PREVIEW`
- Creates `ImageReader` for continuous image capture
- Sends images to AI every 3 seconds using same JSON format
- No TextureView or preview UI required
- Maintains same image processing and scaling logic

### UI Changes
- **Removed**: TextureView from activity_main.xml layout
- **Expanded**: Chat log area takes up more screen space (80% height)
- **Kept**: All control buttons (Start Capture, Start Audio, Stop Audio)
- **Status**: Camera capture works silently in background

## 💡 Tips for Success

- **Always call `cleanUp()`** in lifecycle methods to prevent memory leaks
- **Check both CAMERA and RECORD_AUDIO permissions** before starting
- **Monitor connection status** and implement reconnection logic
- **Use callbacks for UI updates** instead of polling
- **Test audio playback** - should work exactly like MainActivityLegacy.java
- **Test camera capture** - images sent to AI automatically, no preview needed
- **Test thoroughly on different Android versions** and devices
- **Consider implementing retry logic** for network failures
- **Document your customizations** for team members

## 🎯 Migration Success

This migration guide ensures you can successfully use the refactored `AiInteration.java` class in any Android project or Unity plugin while maintaining:
- ✅ **All original functionality** from MainActivityLegacy.java
- ✅ **Working audio playback** (fixed implementation)
- ✅ **Camera video capture** for AI (without preview UI)
- ✅ **Clean separation of concerns** (UI vs AI logic)
- ✅ **Ready for Unity plugin migration** (self-contained class)
- ✅ **Significantly reduced MainActivity** complexity

The implementation now perfectly balances functionality preservation with code organization, making it ideal for both Android development and Unity plugin integration.