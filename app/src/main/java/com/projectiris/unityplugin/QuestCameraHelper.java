package com.projectiris.unityplugin;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

public class QuestCameraHelper {
    private static final String TAG = "QuestCameraHelper";

    // Quest Camera specific characteristic key
    private static final String KEY_CAMERA_POSITION = "com.meta.extra_metadata.position";
    private static final String KEY_CAMERA_SOURCE = "com.meta.extra_metadata.camera_source";
    private static final int CAMERA_SOURCE_PASSTHROUGH = 0;
    private static final int POSITION_RIGHT = 1;

    // Get ID for right eye camera. This code is from Meta-Passthrough-Camera-API-Samples project
    public static String getPassthroughCameraId(CameraManager cameraManager) {
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();

            for (String cameraId : cameraIdList) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

                // Get camera info using the characteristics key acquired above
                Integer cameraSource = getIntegerKey(characteristics, KEY_CAMERA_SOURCE);
                Integer cameraPosition = getIntegerKey(characteristics, KEY_CAMERA_POSITION);

                Log.d(TAG, String.format("Camera ID: %s, Source: %s, Position: %s",
                        cameraId, cameraSource, cameraPosition));

                // Find the right eye camera
                if (cameraSource != null && cameraSource == CAMERA_SOURCE_PASSTHROUGH &&
                        cameraPosition != null && cameraPosition == POSITION_RIGHT) {
                    Log.d(TAG, "Found Passthrough camera: " + cameraId);
                    return cameraId;
                }
            }

            Log.e(TAG, "No Passthrough camera found");
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error finding Passthrough camera", e);
            return null;
        }
    }

    // Get customized characteristic key
    private static Integer getIntegerKey(CameraCharacteristics characteristics, String keyName) {
        try {
            CameraCharacteristics.Key<Integer> key =
                    new CameraCharacteristics.Key<>(keyName, Integer.class);
            return characteristics.get(key);
        } catch (Exception e) {
            Log.w(TAG, "Failed to get key: " + keyName);
            return null;
        }
    }
}