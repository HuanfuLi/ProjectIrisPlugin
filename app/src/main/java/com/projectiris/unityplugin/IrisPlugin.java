package com.projectiris.unityplugin;

import android.app.Activity;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

public class IrisPlugin {
    private static final String TAG = "IrisPluginDebug";
    private static final String UNITY_CALLBACK_OBJECT = "AndroidPluginManager";
    private static final String UNITY_CALLBACK_METHOD = "OnApiResultReceived";

    private static Activity unityActivity;
    private static IrisPlugin instance;

    public static void initialize(Activity activity) {
        unityActivity = activity;
        if (instance == null) {
            instance = new IrisPlugin();
        }
        Log.d(TAG, "IrisPlugin initialized");
    }

    private IrisPlugin() {
        Log.d(TAG, "IrisPlugin constructor called");
    }

    public static void performActionWithApiKey(String ApiKey){
        // null check
        if (unityActivity == null) {
            Log.e(TAG, "UnityActivity is null");
            return;
        }

        // TODO: Perform action with the given ApiKey
        Log.d(TAG, "Performing action with ApiKey: " + ApiKey);

        // Simulating API interaction
        String result = "API result for ApiKey: " + ApiKey + " Success";

        // Send message to Unity
        UnityPlayer.UnitySendMessage(UNITY_CALLBACK_OBJECT, UNITY_CALLBACK_METHOD, result);

    }



}
