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

package com.google.ar.core.examples.java.augmentedimage;

import static com.google.ar.core.examples.java.augmentedimage.ImageConverter.getBitmap;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
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
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.InstantPlacementPoint;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.augmentedimage.rendering.AugmentedImageRenderer;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper;
import com.google.ar.core.examples.java.common.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.common.rendering.PlaneRenderer;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This app extends the HelloAR Java app to include image tracking functionality.
 *
 * <p>In this example, we assume all images are static or moving slowly with a large occupation of
 * the screen. If the target is actively moving, we recommend to check
 * AugmentedImage.getTrackingMethod() and render only when the tracking method equals to
 * FULL_TRACKING. See details in <a
 * href="https://developers.google.com/ar/develop/java/augmented-images/">Recognize and Augment
 * Images</a>.
 */
public class AugmentedImageActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = AugmentedImageActivity.class.getSimpleName();
    public static final ArrayList<String> planets = new ArrayList(Arrays.asList("SONNE", "MERKUR",
            "VENUS", "ERDE", "MARS", "JUPITER", "SATURN", "URANUS", "NEPTUN"));

    private static final String SEARCHING_PLANE_MESSAGE = "Oberflächen werden gesucht...";
    private static final String NO_SIGN_FOUND_MESSAGE = "Kein Schild gefunden...";

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

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final AugmentedImageRenderer augmentedImageRenderer = new AugmentedImageRenderer();

    private boolean shouldConfigureSession = false;

    private boolean takePic = false;

    // Augmented image configuration and rendering.
    // Load a single image (true) or a pre-generated image database (false).
    private final boolean useSingleImage = false;
    // Augmented image and its associated center pose anchor, keyed by index of the augmented image in
    // the database.
    private final Map<Integer, Pair<AugmentedImage, Anchor>> augmentedImageMap = new HashMap<>();

    private CenterPoint planetCenter = null;
    private CenterPoint rectangleCenter = null;
    private int frameNumber = 0;

    private String text = "";
    private Button mButton;
    private boolean planetFound = false;
    private boolean rectangleFound = false;
    private String planet = "";
    private Bitmap currentBitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

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
            System.out.println("SUCCESSFUL INIT");
        } else {
            System.out.println("Fucking OPENCV NOT WORKING AT ALL");
        }

        installRequested = false;
    }

    public void takePic() {
        takePic = true;
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
            Toast.makeText(
                    this, "Camera permissions are needed to run this application", Toast.LENGTH_LONG)
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

            // TODO hasTackingPlane() evtl. falsch
           // if (takePic && hasTrackingPlane()) {
            if (takePic) {
                takePic = false;

                // get image from current frame
                Image image = frame.acquireCameraImage();
                // get jpeg bitmap from YUV image
                currentBitmap = getBitmap(image);

                // analyse text in image
                runTextRecognition(InputImage.fromBitmap(currentBitmap, 90));

                image.close();
            }

            if (planetFound) {
                org.opencv.core.Point pt = new RectangleDetector().detectRectangle(currentBitmap, planetCenter);
                messageSnackbarHelper.showMessage(this, "rect");
                if (pt != null) {
                    rectangleCenter = new CenterPoint((float) pt.x, (float) pt.y);
                    System.out.println("rectangle " + rectangleCenter);
                    rectangleFound = true;
                } else {
                    planetFound = false;
                }
            }

            if (planetFound && rectangleFound) {
                planetFound = false;
                rectangleFound = false;
                messageSnackbarHelper.showMessage(this, "rect: " + rectangleCenter + "  text: " + planetCenter);

                float midX = (planetCenter.getX() + rectangleCenter.getX()) / 2;
                float midY = (planetCenter.getY() + rectangleCenter.getY()) / 2;

                // TODO für Punkt Berechnung evtl. Achsen tauschen mit image.width und height berechenbar
                handleFoundWord(frame, camera, midX, midY, planet);
            }


            if (wrappedAnchor != null) {
                drawPlanet(projmtx, viewmtx, colorCorrectionRgba);
            }

            if ((frameNumber % 90) == 0) {
                takePic = true;
            }
            frameNumber++;
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }


    /** Checks if we detected at least one plane. */
    private boolean hasTrackingPlane() {
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true;
            }
        }
        return false;
    }

    private void writeToFile() {

        if (text.equals("")) {
            return;
        }

        final File out = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/HelloAR", "TXT" + Long.toHexString(System.currentTimeMillis()) + ".txt");

        File path = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS) + "/HelloAR");
        try {
            path.mkdir();
            // Write it to disk.
            FileOutputStream fos = new FileOutputStream(out);

            fos.write(text.getBytes());
            fos.flush();
            fos.close();
            System.out.println("Write successful");

        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void writeToFile(String string) {

        if (string.equals("")) {
            System.out.println("Text is empty");
            return;
        }

        final File out = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/HelloAR", "TXT" + Long.toHexString(System.currentTimeMillis()) + ".txt");

        File path = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS) + "/HelloAR");
        try {
            path.mkdir();
            // Write it to disk.
            FileOutputStream fos = new FileOutputStream(out);

            fos.write(string.getBytes());
            fos.flush();
            fos.close();
            System.out.println("Write successful");

        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
            System.out.println("Fehler beim schreiben");
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

        System.out.println("draw planet earth");
        augmentedImageRenderer.drawPlanet(
                viewmtx, projmtx, wrappedAnchor.getAnchor(), colorCorrectionRgba, wrappedAnchor.getPlanetName());
    }

    /// Text recognition
    private void runTextRecognition(InputImage inputImage) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        takePic = false;

        recognizer.process(inputImage).addOnSuccessListener(
                new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text texts) {
                        text = texts.getText();
                        Tuple result = processTextRecognitionResult(texts);

                        if (result != null) {

                            int imageWidth = inputImage.getWidth();
                            int imageHeight = inputImage.getHeight();

                            float rotatedY = result.getRect().exactCenterX();
                            float rotatedYX= result.getRect().exactCenterY();

                            planetCenter = new CenterPoint(result.getRect().exactCenterX(), result.getRect().exactCenterY());
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
                                System.out.println("FAIL!!!");
                                System.out.println(e.getMessage());
                                e.printStackTrace();
                            }
                        });
    }

    private Tuple processTextRecognitionResult(Text texts) {
        List<Text.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.size() == 0) {
            messageSnackbarHelper.showMessage(this, "No text found");
            return null;
        }

        boolean signHeadlineFound = false;
        boolean planetFound = false;
        Tuple result = null;

        for (int i = 0; i < blocks.size(); i++) {
            List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<Text.Element> elements = lines.get(j).getElements();
                if (findSimilarity(lines.get(j).getText(), "Planetenwanderweg Karlsaue") > 0.5) {
                    signHeadlineFound = true;
                }
                //TODO else continue;
                for (int k = 0; k < elements.size(); k++) {
                    for (String planet: planets) {
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
     * @param X first word
     * @param Y second word
     * @return Levenshtein Distance
     */
    public int getLevenshteinDistance(String X, String Y)
    {
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
                cost = X.charAt(i - 1) == Y.charAt(j - 1) ? 0: 1;
                T[i][j] = Integer.min(Integer.min(T[i - 1][j] + 1, T[i][j - 1] + 1),
                        T[i - 1][j - 1] + cost);
            }
        }

        return T[m][n];
    }

    /***
     * returns percentage of Similarity in respective to the Levenshtein Distance
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


    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private void handleFoundWord(Frame frame, Camera camera, float x, float y, String planet) {

        if (camera.getTrackingState() == TrackingState.TRACKING) {
            //TODO vielleicht muss man Werte tauschen bzw. verrechnen
            List<HitResult> hitResultList = frame.hitTest(x, y);
            /*if (hitResultList.isEmpty()) {
                System.out.println("No hit");
            } else {
                System.out.println("Hit");

            }*/
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
                    // For devices that support the Depth API, shows a dialog to suggest enabling
                    // depth-based occlusion. This dialog needs to be spawned on the UI thread.
                    // this.runOnUiThread(this::showOcclusionDialogIfNeeded);

                    // Hits are sorted by depth. Consider only closest hit on a plane, Oriented Point, or
                    // Instant Placement Point.
                    break;
                }
            }
        }
    }
}

/**
 * Associates an Anchor with the trackable it was attached to. This is used to be able to check
 * whether or not an Anchor originally was attached to an {@link InstantPlacementPoint}.
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

