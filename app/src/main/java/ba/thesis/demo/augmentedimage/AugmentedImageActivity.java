/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ba.thesis.demo.augmentedimage;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.*;

import ba.thesis.demo.augmentedimage.rendering.AugmentedImageRenderer;
import ba.thesis.demo.common.helpers.CameraPermissionHelper;
import ba.thesis.demo.common.helpers.DisplayRotationHelper;
import ba.thesis.demo.common.helpers.FullScreenHelper;
import ba.thesis.demo.common.helpers.SnackbarHelper;
import ba.thesis.demo.common.helpers.TrackingStateHelper;
import ba.thesis.demo.common.rendering.BackgroundRenderer;
import ba.thesis.demo.common.rendering.PlaneRenderer;

import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This app is an adaption of the codeLab ARCore augmented Images from Google.
 * It also uses some code from the codeLab HelloAR.
 * It was modified with OpenCV to be able to detect a rectangle in an image obtained from ARCore.
 * It also uses MLKit for OCR to detect texts. The app was developed in a bachelor thesis and is
 * able to recognise a rectangle with including the text in it. It is used to enhance the signs
 * of the "Planetenwanderweg Karlsaue" with augmented reality. This app implements the OpenCV
 * solution. There is also another project with the augmented images solution.
 * Follow this link to the ARCore website:
 * href="https://developers.google.com/ar/develop/java/augmented-images/">Recognize and Augment
 * * Images</a>.
 */
public class AugmentedImageActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = AugmentedImageActivity.class.getSimpleName();
    public static final ArrayList<String> planets = new ArrayList(Arrays.asList("SONNE", "MERKUR",
            "VENUS", "ERDE", "MARS", "JUPITER", "SATURN", "URANUS", "NEPTUN"));

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;
    private ImageView fitToScanView;
    private RequestManager glideRequestManager;
    private WrappedAnchor wrappedAnchor;

    private boolean installRequested;

    private Session session;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private DisplayRotationHelper displayRotationHelper;
    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
    private boolean found = false;

    private enum ImageResolution {
        LOW_RESOLUTION,
        MEDIUM_RESOLUTION,
        HIGH_RESOLUTION,
    }

    private ImageResolution cpuResolution = ImageResolution.LOW_RESOLUTION;

    // This lock prevents changing resolution as the frame is being rendered. ARCore requires all
    // CPU images to be released before changing resolution.
    private final Object frameImageInUseLock = new Object();

    // For Camera Configuration APIs usage.
    private CameraConfig cpuLowResolutionCameraConfig, cpuMediumResolutionCameraConfig, cpuHighResolutionCameraConfig;

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final AugmentedImageRenderer augmentedImageRenderer = new AugmentedImageRenderer();

    private boolean shouldConfigureSession = false;

    // variable to control scanning process
    private boolean takePic = false;

    // used for synchonization between the treads
    private AtomicReference<org.opencv.core.Point> result = new AtomicReference<>(new org.opencv.core.Point(-1, -1));
    private AtomicReference<Boolean> resultAvailable = new AtomicReference<>(false);

    private int scanRythm = 10;

    // location of the found planetname
    private CenterPoint planetCenter = null;

    // location of the center of the sign
    private CenterPoint rectangleCenter = null;
    private int frameNumber = 0;

    private boolean firstFrame = true;

    // checks if sign is seen or tracked the first time in a session. Is used for time measurements.
    private boolean firstFound = false;
    private boolean firstTracking = false;

    private boolean parallelThreadExecuting = false;

    long startTimestamp = 0;

    private Button mButton;
    private boolean planetFound = false;
    private boolean rectangleFound = false;
    private String planet = "";

    // safes bitmap of current image
    private Bitmap currentBitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        surfaceView.getWidth();
        mButton = findViewById(R.id.button);

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        surfaceView.setWillNotDraw(false);

        fitToScanView = findViewById(R.id.image_view_fit_to_scan);
        glideRequestManager = Glide.with(this);
        glideRequestManager
                .load(Uri.parse("file:///android_asset/fit_to_scan.png"))
                .into(fitToScanView);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePic();
            }
        });


        boolean success = OpenCVLoader.initDebug();
        if (success) {
            System.out.println("SUCCESSFUL OPENCV INIT");
        } else {
            System.out.println("ERROR OPENCV INIT");
        }

        installRequested = false;
    }

    // function invoked by the button. Does not have a function in final version.
    public void takePic() {
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            // Explicitly close ARCore Session to release native resources.
            // Review the API reference for important considerations before calling close() in apps with
            // more complicated lifecycle requirements:
            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
            session.close();
            session = null;
        }

        mButton.setOnClickListener(null);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                session = new Session(/* context = */ this);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (Exception e) {
                message = "This device does not support AR";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

            shouldConfigureSession = true;
        }

        if (shouldConfigureSession) {
            configureSession();
            shouldConfigureSession = false;
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
            session = null;
            return;
        }

        obtainCameraConfigs();
        surfaceView.onResume();
        displayRotationHelper.onResume();

        fitToScanView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            // If you use this file and import it into the augmented images codelab you have
            // to add storage permissions in the CameraPermissionHelper when you want to use
            // Android 13 or higher
            Toast.makeText(
                    this, "Camera and storage permissions are needed to run this application.", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(/*context=*/ this);
            augmentedImageRenderer.createOnGlThread(/*context=*/ this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }

        if (firstFrame) {
            startTimestamp = System.currentTimeMillis();
            firstFrame = false;
        }

        if (cpuResolution == ImageResolution.LOW_RESOLUTION && cpuMediumResolutionCameraConfig != null) {
            onCameraConfigChanged(cpuMediumResolutionCameraConfig);
            cpuResolution = ImageResolution.MEDIUM_RESOLUTION;
        }

        if ((System.currentTimeMillis() - startTimestamp) >= 10000) {
            messageSnackbarHelper.showMessage(this, "Not found in 10s please restart app.");

            if (firstFound && !firstTracking) {
                // writeToFile(-1 + ",", 1, planet);
            }

            firstFound = true;
            firstTracking = true;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);
        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = session.update();
            Camera camera = frame.getCamera();

            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer.draw(frame);

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

            if (takePic) {
                takePic = false;

                // get image from current frame
                Image image = frame.acquireCameraImage();

                // convert to jpeg bitmap from YUV image
                currentBitmap = ImageConverter.getBitmap(image);

                // analyse text in image
                runTextRecognition(InputImage.fromBitmap(currentBitmap, 90));

                image.close();
            }

            // if text was found and no rectangle detection is running
            if (planetFound && !parallelThreadExecuting) {
                RectangleDetectionTask task = new RectangleDetectionTask();
                task.execute(new Tuple2(planetCenter, currentBitmap));
                parallelThreadExecuting = true;
                messageSnackbarHelper.showMessage(this, "search_rect");
            }

            // if rectangle was found
            if (resultAvailable.get()) {
                org.opencv.core.Point resultPoint = result.get();
                resultAvailable.set(false);

                if (resultPoint.x != -1 && resultPoint.y != -1) {
                    rectangleCenter = new CenterPoint((float) resultPoint.x, (float) resultPoint.y);
                    rectangleFound = true;
                } else {
                    parallelThreadExecuting = false;
                    planetFound = false;
                }
            }

            // if planetname and rectangle around were found
            if (planetFound && rectangleFound) {
                planetFound = false;
                rectangleFound = false;
                found = true;
                messageSnackbarHelper.showMessage(this, "Planet: " + planet);

                // calculate x and y value based on screen rotation of 90 degrees
                float x = currentBitmap.getHeight() - rectangleCenter.getY();
                float y = rectangleCenter.getX();

                // switch width and height because image is rotated by 90 degrees
                float imageWidth = currentBitmap.getHeight();
                float imageHeight = currentBitmap.getWidth();

                float displayWidth = surfaceView.getWidth();
                float displayHeight = surfaceView.getHeight();

                // calculate coordinates on screen based on cropping of 16:9 format to display format
                float nWidth = imageHeight * (displayWidth / displayHeight);
                float overflowPerSide = (imageWidth - nWidth) / 2;
                float xTemp = x - overflowPerSide;
                float scaledX = xTemp * (displayWidth / nWidth);

                float scaledY = y * (displayHeight / imageHeight);

                // hitTest with center of rectangle coordinates
                handleFoundWord(frame, camera, scaledX, scaledY, planet);

                // set rhythm to every 4 seconds to increase performance.
                // Rescanning is usually not necessary.
                scanRythm = 120;

                if (found && !firstFound) {
                    firstFound = true;
                    messageSnackbarHelper.showMessage(this, "Found");
                    // writeToFile((System.currentTimeMillis() - startTimestamp) + ",", 0, planet);
                }
            }

            // if planet appears on screen for the first time. Used for time calculation
            if (wrappedAnchor != null && !firstTracking && found) {
                firstTracking = true;
                messageSnackbarHelper.showMessage(this, "Tracking");
                // writeToFile((System.currentTimeMillis() - startTimestamp) + ",", 1, planet);
            }

            if (wrappedAnchor != null) {
                drawPlanet(projmtx, viewmtx, colorCorrectionRgba);
            }

            // start new scan
            if ((frameNumber % scanRythm) == 0) {
                takePic = true;
            }
            frameNumber++;
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }


    // function is used for writing the measured times in the corresponding files
    private void writeToFile(String string, int fileName, String planet) {
        if (string.equals("")) {
            System.out.println("Text is empty");
            return;
        }

        File path = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS) + "/BA_Demo");
        try {
            path.mkdir();
            // Write it to disk.
            File out = null;
            if (fileName == 0) {
                out = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/BA_Demo", "Rect_" + planet + "_found.txt");
            } else {
                out = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/BA_Demo", "Rect_" + planet + "_tracking.txt");
            }
            FileWriter fr = new FileWriter(out, true); // parameter 'true' is for append mode
            fr.write("\n" + string);
            fr.close();

        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void configureSession() {
        Config config = new Config(session);
        config.setFocusMode(Config.FocusMode.AUTO);
        config.setDepthMode(Config.DepthMode.AUTOMATIC);
        config.setLightEstimationMode(Config.LightEstimationMode.AMBIENT_INTENSITY);
        session.configure(config);
    }

    private void drawPlanet(float[] projmtx, float[] viewmtx, float[] colorCorrectionRgba) {

        if (wrappedAnchor == null) {
            return;
        }

        fitToScanView.setVisibility(View.GONE);
        augmentedImageRenderer.drawPlanet(
                viewmtx, projmtx, wrappedAnchor.getAnchor(), colorCorrectionRgba, wrappedAnchor.getPlanetName());
    }

    /**
     * Optical character recognition
     */
    private void runTextRecognition(InputImage inputImage) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(inputImage).addOnSuccessListener(
                new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text texts) {
                        Tuple result = processTextRecognitionResult(texts);
                        if (result != null) {
                            // rotate by 90 degrees counterclockwise
                            planetCenter = new CenterPoint(result.getRect().exactCenterY(),
                                    currentBitmap.getHeight() - result.getRect().exactCenterX());
                            planet = result.getPlanet();
                            planetFound = true;
                        }
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                System.out.println(e.getMessage());
                                e.printStackTrace();
                            }
                        });
    }

    /**
     * Iterates through the obtained texts and search for the predefined terms. If found return
     * the coordinates
     *
     * @param texts of the OCR
     * @return location of planetname
     */
    private Tuple processTextRecognitionResult(Text texts) {
        List<Text.TextBlock> blocks = texts.getTextBlocks();
        boolean signHeadlineFound = false;
        Tuple result = null;

        for (int i = 0; i < blocks.size(); i++) {
            List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<Text.Element> elements = lines.get(j).getElements();
                if (findSimilarity(lines.get(j).getText(), "Planetenwanderweg Karlsaue") > 0.5) {
                    signHeadlineFound = true;
                }
                for (int k = 0; k < elements.size(); k++) {
                    for (String planet : planets) {
                        if (elements.get(k).getText().contains(planet)) {
                            result = new Tuple(planet, elements.get(k).getBoundingBox());
                            if (signHeadlineFound) {
                                return result;
                            }
                        }
                    }
                }
            }
        }
        if (signHeadlineFound) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * Levenshtein Distance describes number of chars that need to be changed to transform x to y
     *
     * @param X first word
     * @param Y second word
     * @return Levenshtein Distance
     */
    public int getLevenshteinDistance(String X, String Y) {
        int m = X.length();
        int n = Y.length();

        int[][] T = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            T[i][0] = i;
        }
        for (int j = 1; j <= n; j++) {
            T[0][j] = j;
        }

        int cost;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                cost = X.charAt(i - 1) == Y.charAt(j - 1) ? 0 : 1;
                T[i][j] = Integer.min(Integer.min(T[i - 1][j] + 1, T[i][j - 1] + 1),
                        T[i - 1][j - 1] + cost);
            }
        }

        return T[m][n];
    }

    /***
     * Returns percentage of Similarity in respective to the Levenshtein Distance
     */
    public double findSimilarity(String x, String y) {
        if (x == null || y == null) {
            throw new IllegalArgumentException("Strings must not be null");
        }

        double maxLength = Double.max(x.length(), y.length());
        if (maxLength > 0) {
            // optionally ignore case if needed
            return (maxLength - getLevenshteinDistance(x, y)) / maxLength;
        }
        return 1.0;
    }


    /**
     * Simulates tap on screen with th given coordinates. Is used for getting an anchor.
     */
    private void handleFoundWord(Frame frame, Camera camera, float x, float y, String planet) {
        if (camera.getTrackingState() == TrackingState.TRACKING) {
            List<HitResult> hitResultList = frame.hitTest(x, y);
            for (HitResult hit : hitResultList) {
                // If any plane, Oriented Point, or Instant Placement Point was hit, create an anchor.
                Trackable trackable = hit.getTrackable();
                // If a plane was hit, check that it was hit inside the plane polygon.
                // DepthPoints are only returned if Config.DepthMode is set to AUTOMATIC.
                if ((trackable instanceof Plane
                        && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                        && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
                        || (trackable instanceof Point
                        && ((Point) trackable).getOrientationMode()
                        == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
                        || (trackable instanceof InstantPlacementPoint)
                        || (trackable instanceof DepthPoint)) {

                    // Adding an Anchor tells ARCore that it should track this position in
                    // space. This anchor is created on the Plane to place the 3D model
                    // in the correct position relative both to the world and to the plane.
                    if (wrappedAnchor != null) {
                        wrappedAnchor.getAnchor().detach();
                    }
                    wrappedAnchor = new WrappedAnchor(hit.createAnchor(), trackable, planet);
                    break;
                }
            }
        }
    }

    private void onCameraConfigChanged(CameraConfig cameraConfig) {
        // To change the AR camera config - first we pause the AR session, set the desired camera
        // config and then resume the AR session.
        if (session != null) {
            // Block here if the image is still being used.
            synchronized (frameImageInUseLock) {
                session.pause();
                session.setCameraConfig(cameraConfig);
                try {
                    session.resume();
                } catch (CameraNotAvailableException ex) {
                    messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
                    session = null;
                }
            }
        }
    }

    /**
     * Obtains the supported camera configs
     */
    private void obtainCameraConfigs() {
        // First obtain the session handle before getting the list of various camera configs.
        if (session != null) {
            // Create filter here with desired fps filters.
            CameraConfigFilter cameraConfigFilter =
                    new CameraConfigFilter(session)
                            .setTargetFps(
                                    EnumSet.of(
                                            CameraConfig.TargetFps.TARGET_FPS_30, CameraConfig.TargetFps.TARGET_FPS_60));
            List<CameraConfig> cameraConfigs = session.getSupportedCameraConfigs(cameraConfigFilter);
            Log.i(TAG, "Size of supported CameraConfigs list is " + cameraConfigs.size());

            // Determine the highest and lowest CPU resolutions.
            cpuLowResolutionCameraConfig =
                    getCameraConfigWithSelectedResolution(
                            cameraConfigs, /*ImageResolution*/ ImageResolution.LOW_RESOLUTION);
            cpuMediumResolutionCameraConfig =
                    getCameraConfigWithSelectedResolution(
                            cameraConfigs, /*ImageResolution*/ ImageResolution.MEDIUM_RESOLUTION);
            cpuHighResolutionCameraConfig =
                    getCameraConfigWithSelectedResolution(
                            cameraConfigs, /*ImageResolution*/ ImageResolution.HIGH_RESOLUTION);
        }
    }

    /**
     * Get the CameraConfig with selected resolution.
     **/
    private static CameraConfig getCameraConfigWithSelectedResolution(
            List<CameraConfig> cameraConfigs, ImageResolution resolution) {
        // Take the first three camera configs, if camera configs size are larger than 3.
        List<CameraConfig> cameraConfigsByResolution =
                new ArrayList<>(
                        cameraConfigs.subList(0, Math.min(cameraConfigs.size(), 3)));
        Collections.sort(
                cameraConfigsByResolution,
                (CameraConfig p1, CameraConfig p2) ->
                        Integer.compare(p1.getImageSize().getHeight(), p2.getImageSize().getHeight()));
        CameraConfig cameraConfig = cameraConfigsByResolution.get(0);
        switch (resolution) {
            case LOW_RESOLUTION:
                cameraConfig = cameraConfigsByResolution.get(0);
                break;
            case MEDIUM_RESOLUTION:
                // There are some devices that medium resolution is the same as high resolution.
                cameraConfig = cameraConfigsByResolution.get(1);
                break;
            case HIGH_RESOLUTION:
                cameraConfig = cameraConfigsByResolution.get(2);
                break;
        }
        return cameraConfig;
    }


    /**
     * Asnyc task for the recognition of the rectangle. Calculates its center.
     **/
    private class RectangleDetectionTask extends AsyncTask<Tuple2, Void, org.opencv.core.Point> {
        @Override
        protected org.opencv.core.Point doInBackground(Tuple2... tuple2s) {
            return new RectangleDetector().detectRectangle(tuple2s[0].getBitmap(), tuple2s[0].getPlanet());
        }

        @Override
        protected void onPostExecute(org.opencv.core.Point point) {
            if (point != null) {
                result.set(point);
                resultAvailable.set(true);
            }
            parallelThreadExecuting = false;
        }
    }
}

/**
 * Associates an Anchor with the trackable and planetName it was attached to.
 */
class WrappedAnchor {
    private final Anchor anchor;
    private final Trackable trackable;
    private final String planetName;

    public WrappedAnchor(Anchor anchor, Trackable trackable, String planetName) {
        this.anchor = anchor;
        this.trackable = trackable;
        this.planetName = planetName;
    }

    public Anchor getAnchor() {
        return anchor;
    }

    public Trackable getTrackable() {
        return trackable;
    }

    public String getPlanetName() {
        return planetName;
    }
}

/**
 * Defines a datastructure that is used to associate a Rectangle to a planetName
 */
final class Tuple {
    private final String planet;
    private final Rect rect;

    Tuple(String planet, Rect rect) {
        this.planet = planet;
        this.rect = rect;
    }

    public String getPlanet() {
        return this.planet;
    }

    public Rect getRect() {
        return this.rect;
    }
}

/**
 * Defines a datastructure that is used to associate a bitmap to a planetName.
 * Is used in the asynchronous rectangleDetectionTask
 */
final class Tuple2 {
    private final CenterPoint planet;
    private final Bitmap bitmap;

    Tuple2(CenterPoint planet, Bitmap bitmap) {
        this.planet = planet;
        this.bitmap = bitmap;
    }

    public CenterPoint getPlanet() {
        return this.planet;
    }

    public Bitmap getBitmap() {
        return this.bitmap;
    }
}


final class CenterPoint {
    private final float x;
    private final float y;

    CenterPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    @NonNull
    @Override
    public String toString() {
        return "CenterPoint{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}



