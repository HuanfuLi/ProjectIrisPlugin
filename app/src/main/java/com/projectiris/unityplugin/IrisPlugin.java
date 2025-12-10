package com.projectiris.unityplugin;

import android.Manifest;
import android.app.Activity;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.unity3d.player.UnityPlayer;

// This class is a JNI used as a bridge to communicate with Unity.
public class IrisPlugin {
    // Hard Code API_KEY
    // private static final String API_KEY = "AIzaSyDbsXMnCEAOQWJSOsWdZ0PAbXy2L4IC-Ts";

    private static final String TAG = "IrisPluginDebug";
    private static final String UNITY_CALLBACK_OBJECT = "AndroidPluginManager";
    private static final String UNITY_CALLBACK_METHOD = "OnApiResultReceived";
    private static final String UNITY_MESSAGE_CALLBACK_METHOD = "OnMessageReceived";

    private static Activity unityActivity;
    private static IrisPlugin instance;
    private static AiInteration aiInteraction;

    // Initialize the plugin with Unity activity and API key
    public static void initialize(Activity activity, boolean isMuted, String API_KEY) {
        unityActivity = activity;
        if (instance == null) {
            instance = new IrisPlugin();
        }
        Log.d(TAG, "IrisPlugin initialized"); // Log message to check if the plugin is initialized
        
        if (aiInteraction == null) {
            aiInteraction = new AiInteration(API_KEY, isMuted);
            aiInteraction.setMessageCallback((message, type) -> {
                displayMessage(message, type);
            });
        }
    }

    // Constructor
    private IrisPlugin() {
        Log.d(TAG, "IrisPlugin constructor called"); // Log message to check if the constructor is called
    }

    // Public methods used by outside code (Plugin/Main)
    public static void startConnection() {
        if (aiInteraction != null) {
            aiInteraction.start();
            Log.d(TAG, "AI connection started");
        } else {
            Log.e(TAG, "AiInteraction not initialized");
        }
    }

    // Stop the AI connection
    public static void stopConnection() {
        if (aiInteraction != null) {
            aiInteraction.stop();
            Log.d(TAG, "AI connection stopped");
        }
    }

    // Start camera for capture
    // This method is used to start camera for capture
    @RequiresPermission(Manifest.permission.CAMERA)
    public static void startCamera() {
        if (aiInteraction != null && unityActivity != null) {
            aiInteraction.startCamera(unityActivity);
            Log.d(TAG, "Camera started");
        } else {
            Log.e(TAG, "AiInteraction or UnityActivity is null");
        }
    }

    // Stop camera
    // This method is used to stop camera
    public static void stopCamera() {
        if (aiInteraction != null) {
            aiInteraction.stopCamera();
            Log.d(TAG, "Camera stopped");
        }
    }

    // Start audio input
    // This method is used to start audio input
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public static void startRecording() {
        if (aiInteraction != null) {
            aiInteraction.startRecording();
            Log.d(TAG, "Audio recording started");
        } else {
            Log.e(TAG, "AiInteraction not initialized");
        }
    }

    // Stop audio input
    // This method is used to stop audio input
    public static void stopRecording() {
        if (aiInteraction != null) {
            aiInteraction.stopRecording();
            Log.d(TAG, "Audio recording stopped");
        }
    }

    // Check whether AI is connected
    // This method is used to check whether AI is connected
    public static boolean isConnected() {
        return aiInteraction != null && aiInteraction.isConnected();
    }

    // Check whether audio is recording
    // This method is used to check whether audio is recording
    public static boolean isRecording() {
        return aiInteraction != null && aiInteraction.isRecording();
    }

    // Check whether camera is active
    // This method is used to check whether camera is active
    public static boolean isCameraActive() {
        return aiInteraction != null && aiInteraction.isCameraActive();
    }

    // send image to AI
    public static void sendImage(byte[] imageBytes) {
        if (aiInteraction != null) {
            aiInteraction.sendImage(imageBytes);
            Log.d(TAG, "Image sent to AI");
        } else {
            Log.e(TAG, "AiInteraction not initialized");
        }
    }

    // Set muted mode
    // This method is used to switch AI output model
    public static void setMuted(boolean muted) {
        if (aiInteraction != null) {
            aiInteraction.setMuted(muted);
            Log.d(TAG, "Muted mode set to: " + muted);
        }
    }

    // Check whether AI is muted
    public static boolean isMuted() {
        return aiInteraction != null && aiInteraction.isMuted();
    }

    // Clean up the plugin
    public static void cleanUp() {
        if (aiInteraction != null) {
            aiInteraction.cleanUp();
            aiInteraction = null;
            Log.d(TAG, "AiInteraction cleaned up");
        }
    }

    // Escape JSON string to prevent injection
    private static String escapeJsonString(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    // Send message to Unity
    private static void displayMessage(String message, String type) {
        String text = escapeJsonString(message);
        Log.d(TAG, "Message received - Type: " + type + ", Content: " + message); // Log message to check if the message is received

        try {
            // parse jsonMessage and sent to Unity
            // Format: {"type":"type","message":"message"}
            String jsonMessage = String.format("{\"type\":\"%s\",\"message\":\"%s\"}", type, text);
            UnityPlayer.UnitySendMessage(UNITY_CALLBACK_OBJECT, UNITY_MESSAGE_CALLBACK_METHOD, jsonMessage);
        } catch (Exception e) {
            Log.e(TAG, "Error sending message to Unity", e); // Log error message to check if the message is sent
        }
    }
}
