package com.projectiris.unityplugin;

// Imports

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiInteration {
    private static final String TAG = "AiInteration";
    private static final String textTAG = "textOutput";
    
    // Message callback interface for UI updates
    public interface MessageCallback {
        void onMessageReceived(String message, String type);
    }
    
    private MessageCallback messageCallback;
    private String API_KEY;
    private boolean muted;
    private static final String MODEL = "models/gemini-2.0-flash-exp";
    private static final String HOST = "generativelanguage.googleapis.com";
    public String URL;

    // Constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int AUDIO_REQUEST_CODE = 200;
    private static final int AUDIO_SAMPLE_RATE = 24000;
    private static final int RECEIVE_SAMPLE_RATE = 24000;
    private static final int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MAX_IMAGE_DIMENSION = 1024;
    private static final int JPEG_QUALITY = 70;
    private static final long IMAGE_SEND_INTERVAL = 3000; // 3 seconds

    // Audio
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private int audioBufferSize;
    private List<Short> pcmData;
    private List<byte[]> audioQueue;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private boolean isSpeaking = false;

    // WebSocket
    private WebSocketClient webSocket;
    private boolean isConnected = false;

    // Camera
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private ImageReader imageReader;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private String cameraId;
    private Size previewSize;
    private boolean isCameraActive = false;
    private long lastImageSendTime = 0;

    // Threading
    private ExecutorService executorService;
    private SimpleDateFormat timeFormat;

    //Constructor
    public AiInteration(String API_KEY, boolean muted){
        this.API_KEY = API_KEY;
        this.muted = muted;
        URL = "wss://" + HOST +
                        "/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=" +
                        API_KEY;
        initializeVariables();
    }

    // Setters and Getters
    public String getAPI_KEY() {
        return API_KEY;
    }

    public void setAPI_KEY(String API_KEY) {
        this.API_KEY = API_KEY;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }


    // Public methods used by outside code (Plugin/Main)
    public void start(){ connect();}
    public void stop(){ disconnect();}
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public void startRecording(){ startAudioInput();}
    public void stopRecording(){ stopAudioInput();}
    public void sendImage(byte[] imageBytes){ processAndSendImage(imageBytes);}
    public boolean isConnected(){ return isConnected;}
    public boolean isRecording(){ return isRecording;}
    public void setMessageCallback(MessageCallback callback){ this.messageCallback = callback;}
    
    // Camera methods
    @RequiresPermission(Manifest.permission.CAMERA)
    public void startCamera(Context context){ openCameraForCapture(context);}
    public void stopCamera(){ stopCameraPreview();}
    public boolean isCameraActive(){ return isCameraActive;}


    private void initializeVariables() {
        audioBufferSize = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL_CONFIG,
                AUDIO_ENCODING
        );

        pcmData = Collections.synchronizedList(new ArrayList<>());
        audioQueue = Collections.synchronizedList(new ArrayList<>());
        executorService = Executors.newCachedThreadPool();
        timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());

        cameraThread = new HandlerThread("CameraThread");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
    }

    private void connect() {
        Log.d(TAG, "Connecting to: " + URL);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        webSocket = new WebSocketClient(URI.create(URL), new Draft_6455(), headers) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.d(TAG, "Connected. Server handshake: " + handshakedata.getHttpStatus());
                isConnected = true;

                // Inform Unity that connection has been made
                if (messageCallback != null) {
                    messageCallback.onMessageReceived("Websocket connected", "connection");
                }

                sendInitialSetupMessage();
            }

            @Override
            public void onMessage(String message) {
                Log.d(TAG, "Message Received: " + message);
                receiveMessage(message);
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                String message = new String(bytes.array(), java.nio.charset.StandardCharsets.UTF_8);
                receiveMessage(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d(TAG, "Connection Closed: " + reason);
                isConnected = false;

                // Inform Unity that connection has been closed
                if (messageCallback != null) {
                    messageCallback.onMessageReceived("Websocket disconnected: " + reason, "connection");
                }
            }

            @Override
            public void onError(Exception ex) {
                String errorMsg = ex != null ? ex.getMessage() : "Unknown error";
                Log.e(TAG, "Error: " + errorMsg);
                isConnected = false;

                // Inform Unity that Error occurred
                if (messageCallback != null) {
                    messageCallback.onMessageReceived("Websocket error: " + errorMsg, "error");
                }
            }
        };
        webSocket.connect();
    }

    private void disconnect(){
        if (webSocket != null){
            webSocket.close();
            webSocket = null;
        }
        isConnected = false;
    }

    private void sendInitialSetupMessage(){
        Log.d(TAG, "Sending initial setup message");

        try{
            JSONObject setupMessage = new JSONObject();
            JSONObject setup = new JSONObject();
            JSONObject generationConfig = new JSONObject();
            JSONArray responseModalities = new JSONArray();

            if (isMuted()){
                responseModalities.put("TEXT");
            }
            generationConfig.put("responseModalities", responseModalities);
            setup.put("model", MODEL);
            setup.put("generationConfig", generationConfig);

            if (!isMuted()){
                responseModalities.put("AUDIO");
                setup.put("outputAudioTranscription", new JSONObject());
            }

            setupMessage.put("setup", setup);

            Log.d(TAG, "Sending config payload: " + setupMessage.toString());
            webSocket.send(setupMessage.toString());

        } catch (JSONException e){
            Log.e(TAG, "Error creating setup message", e);
        }
    }

    private void sendMediaChunk(String b64Data, String mimeType) {
        if (!isConnected) {
            Log.d(TAG, "WebSocket not connected");
            return;
        }

        try {
            JSONObject msg = new JSONObject();
            JSONObject realtimeInput = new JSONObject();
            JSONArray mediaChunks = new JSONArray();
            JSONObject chunk = new JSONObject();

            chunk.put("mime_type", mimeType);
            chunk.put("data", b64Data);
            mediaChunks.put(chunk);
            realtimeInput.put("media_chunks", mediaChunks);
            msg.put("realtime_input", realtimeInput);

            webSocket.send(msg.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error creating media chunk message", e);
        }
    }

    private void receiveMessage(String message) {
        if (message == null) {
            return;
        }

        try {
            JSONObject messageData = new JSONObject(message);

            if (messageData.has("serverContent")) {
                JSONObject serverContent = messageData.getJSONObject("serverContent");

                // a) 模型“语音输出”的文字转写（官方字段：outputTranscription.text）
                if (serverContent.has("outputTranscription")) {
                    JSONObject outTx = serverContent.getJSONObject("outputTranscription");
                    String t = outTx.optString("text", "");
                    if (!t.isEmpty()) {
                        Log.d(textTAG, t); // <<=== 你要的输出
                        if (messageCallback != null) {
                            messageCallback.onMessageReceived("GEMINI (transcript): " + t, "transcript");
                        }
                    }
                }

                if (serverContent.has("modelTurn")) {
                    JSONObject modelTurn = serverContent.getJSONObject("modelTurn");
                    if (modelTurn.has("parts")) {
                        JSONArray parts = modelTurn.getJSONArray("parts");
                        for (int i = 0; i < parts.length(); i++) {
                            JSONObject part = parts.getJSONObject(i);

                            // 模型直接返回的文本（也一起打到 textTAG，便于调试）
                            if (part.has("text")) {
                                String text = part.getString("text");
                                Log.d(textTAG, text); // <<=== 也打印
                                if (messageCallback != null) {
                                    messageCallback.onMessageReceived("GEMINI: " + text, "text");
                                }
                            }

                            // 模型返回的音频
                            if (part.has("inlineData")) {
                                JSONObject inlineData = part.getJSONObject("inlineData");
                                if ("audio/pcm;rate=24000".equals(inlineData.optString("mimeType"))) {
                                    String audioData = inlineData.optString("data", null);
                                    ingestAudioChunkToPlay(audioData);
                                }
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing message", e);
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private void openCameraForCapture(Context context) {
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            // Get the correct camera using QuestCameraHelper.
            cameraId = QuestCameraHelper.getPassthroughCameraId(cameraManager);

            if (cameraId == null) {
                Log.e(TAG, "No Passthrough camera found");
                if (messageCallback != null) {
                    messageCallback.onMessageReceived("No Passthrough camera found", "error");
                }
                return;
            }

            Log.d(TAG, "Camera ID: " + cameraId);

            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            );

            if (map == null) {
                Log.e(TAG, "No Stream Configuration Map found");
                return;
            }

            Size[] jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
            int width = Math.min(jpegSizes[0].getWidth(), MAX_IMAGE_DIMENSION);
            int height = Math.min(jpegSizes[0].getHeight(), MAX_IMAGE_DIMENSION);

            // Create ImageReader for capturing images
            imageReader = ImageReader.newInstance(
                    width,
                    height,
                    ImageFormat.JPEG,
                    2
            );
            imageReader.setOnImageAvailableListener(imageAvailableListener, cameraHandler);

            // Open camera for capture only (no preview)
            cameraManager.openCamera(cameraId, cameraStateCallback, cameraHandler);
            
            Log.d(TAG, "Camera capture started (no preview)");
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error opening camera for capture", e);
        }
    }
    
    private void stopCameraPreview() {
        closeCamera();
        isCameraActive = false;
    }


    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
            Log.e(TAG, "Camera error: " + error);
        }
    };

    private void createCameraPreviewSession() {
        try {
            // Create capture request for still pictures (no preview)
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(imageReader.getSurface());

            // Create capture session with only ImageReader surface (no preview)
            cameraDevice.createCaptureSession(
                    Arrays.asList(imageReader.getSurface()),
                    cameraCaptureSessionCallback,
                    cameraHandler
            );

        } catch (CameraAccessException e) {
            Log.e(TAG, "Error creating capture session", e);
        }
    }

    private final CameraCaptureSession.StateCallback cameraCaptureSessionCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            };

    private void updatePreview() {
        if (cameraDevice == null) return;

        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        isCameraActive = true;

        try {
            // Start repeating capture for continuous image capture (no preview)
            cameraCaptureSession.setRepeatingRequest(
                    captureRequestBuilder.build(),
                    null,
                    cameraHandler
            );
            Log.d(TAG, "Camera capture session started successfully");
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error starting capture repeat request", e);
        }
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    private final ImageReader.OnImageAvailableListener imageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastImageSendTime >= IMAGE_SEND_INTERVAL) {
                        android.media.Image image = reader.acquireLatestImage();
                        if (image == null) return;

                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        image.close();

                        executorService.execute(() -> processAndSendImage(bytes));
                        lastImageSendTime = currentTime;
                        Log.d(TAG, "Image processed and sent based on time interval");
                    } else {
                        android.media.Image image = reader.acquireLatestImage();
                        if (image != null) image.close();
                        Log.d(TAG, "Image capture skipped: Not enough time elapsed");
                    }
                }
            };

    private void processAndSendImage(byte[] imageBytes) {
        String currentTime = timeFormat.format(new Date());
        Log.d(TAG, "Image processed and sending at: " + currentTime);

        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Bitmap scaledBitmap = scaleBitmap(bitmap, MAX_IMAGE_DIMENSION);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, byteArrayOutputStream);

        String b64Image = Base64.encodeToString(
                byteArrayOutputStream.toByteArray(),
                Base64.DEFAULT | Base64.NO_WRAP
        );

        sendMediaChunk(b64Image, "image/jpeg");

        scaledBitmap.recycle();
        try {
            byteArrayOutputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing stream", e);
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap;
        }

        int newWidth;
        int newHeight;

        if (width > height) {
            float ratio = (float) width / maxDimension;
            newWidth = maxDimension;
            newHeight = (int) (height / ratio);
        } else {
            float ratio = (float) height / maxDimension;
            newHeight = maxDimension;
            newWidth = (int) (width / ratio);
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void startAudioInput() {
        if (isRecording) return;

        isRecording = true;
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL_CONFIG,
                AUDIO_ENCODING,
                audioBufferSize
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed");
            return;
        }

        audioRecord.startRecording();
        Log.d(TAG, "Start Recording");
        isSpeaking = true;

        executorService.execute(() -> {
            while (isRecording) {
                short[] buffer = new short[audioBufferSize];
                int readSize = audioRecord.read(buffer, 0, buffer.length);

                if (readSize > 0) {
                    synchronized (pcmData) {
                        for (int i = 0; i < readSize; i++) {
                            pcmData.add(buffer[i]);
                        }
                        if (pcmData.size() >= readSize) {
                            recordChunk();
                        }
                    }
                }
            }
        });
    }

    // This method stops audio input. Invoked when User trigger hand gesture again.
    public void stopAudioInput() {
        if (isRecording) {
            isRecording = false;
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            Log.d(TAG, "Stop Recording");
            isSpeaking = false;
        }
    }

    private void recordChunk() {
        List<Short> dataToSend;
        synchronized (pcmData) {
            if (pcmData.isEmpty()) return;
            dataToSend = new ArrayList<>(pcmData);
            pcmData.clear();
        }

        executorService.execute(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(dataToSend.size() * 2)
                    .order(ByteOrder.LITTLE_ENDIAN);

            for (Short value : dataToSend) {
                buffer.putShort(value);
            }

            byte[] byteArray = buffer.array();
            String base64 = Base64.encodeToString(byteArray, Base64.DEFAULT | Base64.NO_WRAP);
            Log.d(TAG, "Send Audio Chunk");
            sendMediaChunk(base64, "audio/pcm");
        });
    }

    private void ingestAudioChunkToPlay(String base64AudioChunk) {
        if (base64AudioChunk == null) return;

        executorService.execute(() -> {
            try {
                byte[] arrayBuffer = base64ToArrayBuffer(base64AudioChunk);
                synchronized (audioQueue) {
                    audioQueue.add(arrayBuffer);
                }
                if (!isPlaying) {
                    playNextAudioChunk();
                }
                Log.d(TAG, "Audio chunk added to the queue");
            } catch (Exception e) {
                Log.e(TAG, "Error processing chunk", e);
            }
        });
    }

    private void playNextAudioChunk() {
        executorService.execute(() -> {
            while (true) {
                byte[] chunk;
                synchronized (audioQueue) {
                    if (audioQueue.isEmpty()) break;
                    chunk = audioQueue.remove(0);
                }

                isPlaying = true;
                playAudio(chunk);
            }
            isPlaying = false;

            // Check for new chunks that might have arrived while we were exiting
            synchronized (audioQueue) {
                if (!audioQueue.isEmpty()) {
                    playNextAudioChunk();
                }
            }
        });
    }

    private void playAudio(byte[] byteArray) {
        if (audioTrack == null) {
            audioTrack = new AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    RECEIVE_SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioTrack.getMinBufferSize(
                            RECEIVE_SAMPLE_RATE,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT
                    ),
                    AudioTrack.MODE_STREAM
            );
        }

        audioTrack.write(byteArray, 0, byteArray.length);
        audioTrack.play();

        executorService.execute(() -> {
            while (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            audioTrack.stop();
        });
    }

    private byte[] base64ToArrayBuffer(String base64) {
        return Base64.decode(base64, Base64.DEFAULT);
    }

    // Clean up resources
    public void cleanUp(){
        stopRecording();
        disconnect();

        if (executorService != null){
            executorService.shutdown();
        }

        // Clear audio queue
        synchronized (audioQueue) {
            audioQueue.clear();
        }
        
        // Clear PCM data
        synchronized (pcmData) {
            pcmData.clear();
        }

        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }

        if (cameraThread != null){
            cameraThread.quitSafely();
        }

        closeCamera();
    }
}
