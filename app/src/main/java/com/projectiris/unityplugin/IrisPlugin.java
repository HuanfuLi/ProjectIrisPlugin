package com.projectiris.unityplugin;

import android.Manifest;
import android.app.Activity;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.unity3d.player.UnityPlayer;

public class IrisPlugin {
    // Hard Code API_KEY
    // private static final String API_KEY = "AIzaSyDbsXMnCEAOQWJSOsWdZ0PAbXy2L4IC-Ts";

    //private static String API_KEY;
    //private static boolean muted;
    private static final String TAG = "IrisPluginDebug";
    private static final String UNITY_CALLBACK_OBJECT = "AndroidPluginManager";
    private static final String UNITY_CALLBACK_METHOD = "OnApiResultReceived";
    private static final String UNITY_MESSAGE_CALLBACK_METHOD = "OnMessageReceived";

    private static Activity unityActivity;
    private static IrisPlugin instance;
    private static AiInteration aiInteraction;

    public static void initialize(Activity activity, boolean isMuted, String API_KEY) {
        unityActivity = activity;
        if (instance == null) {
            instance = new IrisPlugin();
        }
        Log.d(TAG, "IrisPlugin initialized");
        
        if (aiInteraction == null) {
            aiInteraction = new AiInteration(API_KEY, isMuted);
            aiInteraction.setMessageCallback((message, type) -> {
                displayMessage(message, type);
            });
        }
    }

    private IrisPlugin() {
        Log.d(TAG, "IrisPlugin constructor called");
    }

    public static void performActionWithApiKey(String ApiKey) {
        if (unityActivity == null) {
            Log.e(TAG, "UnityActivity is null");
            return;
        }

        if (aiInteraction == null) {
            Log.e(TAG, "AiInteraction not initialized");
            return;
        }

        Log.d(TAG, "Performing action with ApiKey: " + ApiKey);
        
        try {
            String result = "API initialized successfully with ApiKey: " + ApiKey;
            UnityPlayer.UnitySendMessage(UNITY_CALLBACK_OBJECT, UNITY_CALLBACK_METHOD, result);
        } catch (Exception e) {
            Log.e(TAG, "Error performing action with ApiKey", e);
            UnityPlayer.UnitySendMessage(UNITY_CALLBACK_OBJECT, UNITY_CALLBACK_METHOD, "Error: " + e.getMessage());
        }
    }

    public static void startConnection() {
        if (aiInteraction != null) {
            aiInteraction.start();
            Log.d(TAG, "AI connection started");
        } else {
            Log.e(TAG, "AiInteraction not initialized");
        }
    }

    public static void stopConnection() {
        if (aiInteraction != null) {
            aiInteraction.stop();
            Log.d(TAG, "AI connection stopped");
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public static void startCamera() {
        if (aiInteraction != null && unityActivity != null) {
            aiInteraction.startCamera(unityActivity);
            Log.d(TAG, "Camera started");
        } else {
            Log.e(TAG, "AiInteraction or UnityActivity is null");
        }
    }

    public static void stopCamera() {
        if (aiInteraction != null) {
            aiInteraction.stopCamera();
            Log.d(TAG, "Camera stopped");
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public static void startRecording() {
        if (aiInteraction != null) {
            aiInteraction.startRecording();
            Log.d(TAG, "Audio recording started");
        } else {
            Log.e(TAG, "AiInteraction not initialized");
        }
    }

    public static void stopRecording() {
        if (aiInteraction != null) {
            aiInteraction.stopRecording();
            Log.d(TAG, "Audio recording stopped");
        }
    }

    public static boolean isConnected() {
        return aiInteraction != null && aiInteraction.isConnected();
    }

    public static boolean isRecording() {
        return aiInteraction != null && aiInteraction.isRecording();
    }

    public static boolean isCameraActive() {
        return aiInteraction != null && aiInteraction.isCameraActive();
    }

    public static void sendImage(byte[] imageBytes) {
        if (aiInteraction != null) {
            aiInteraction.sendImage(imageBytes);
            Log.d(TAG, "Image sent to AI");
        } else {
            Log.e(TAG, "AiInteraction not initialized");
        }
    }

    public static void setMuted(boolean muted) {
        if (aiInteraction != null) {
            aiInteraction.setMuted(muted);
            Log.d(TAG, "Muted mode set to: " + muted);
        }
    }

    public static boolean isMuted() {
        return aiInteraction != null && aiInteraction.isMuted();
    }

    public static void cleanUp() {
        if (aiInteraction != null) {
            aiInteraction.cleanUp();
            aiInteraction = null;
            Log.d(TAG, "AiInteraction cleaned up");
        }
    }

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

    private static void displayMessage(String message, String type) {
        String text = escapeJsonString(message);
        Log.d(TAG, "Message received - Type: " + type + ", Content: " + message);

        try {
            String jsonMessage = String.format("{\"type\":\"%s\",\"message\":\"%s\"}", type, text);
            UnityPlayer.UnitySendMessage(UNITY_CALLBACK_OBJECT, UNITY_MESSAGE_CALLBACK_METHOD, jsonMessage);
        } catch (Exception e) {
            Log.e(TAG, "Error sending message to Unity", e);
        }
    }
}
