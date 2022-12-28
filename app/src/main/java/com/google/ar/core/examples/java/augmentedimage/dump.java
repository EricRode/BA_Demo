package com.google.ar.core.examples.java.augmentedimage;

import com.google.ar.core.Camera;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class dump {

     /* private boolean setupAugmentedImageDatabase(Config config) {
        AugmentedImageDatabase augmentedImageDatabase;

        // There are two ways to configure an AugmentedImageDatabase:
        // 1. Add Bitmap to DB directly
        // 2. Load a pre-built AugmentedImageDatabase
        // Option 2) has
        // * shorter setup time
        // * doesn't require images to be packaged in apk.
        if (useSingleImage) {
            Bitmap augmentedImageBitmap = loadAugmentedImageBitmap();
            if (augmentedImageBitmap == null) {
                return false;
            }

            augmentedImageDatabase = new AugmentedImageDatabase(session);
            augmentedImageDatabase.addImage("image_name", augmentedImageBitmap);
            // If the physical size of the image is known, you can instead use:
            //     augmentedImageDatabase.addImage("image_name", augmentedImageBitmap, widthInMeters);
            // This will improve the initial detection speed. ARCore will still actively estimate the
            // physical size of the image as it is viewed from multiple viewpoints.
        } else {
            // This is an alternative way to initialize an AugmentedImageDatabase instance,
            // load a pre-existing augmented image database.
            try (InputStream is = getAssets().open("all.imgdb")) {
                augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, is);
            } catch (IOException e) {
                Log.e(TAG, "IO exception loading augmented image database.", e);
                return false;
            }
        }

        config.setAugmentedImageDatabase(augmentedImageDatabase);
        return true;
    }*/

    /*private Bitmap loadAugmentedImageBitmap() {
        try (InputStream is = getAssets().open("default.jpg")) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e);
        }
        return null;
    }*/


     /*private void drawAugmentedImages(
            Frame frame, float[] projmtx, float[] viewmtx, float[] colorCorrectionRgba) {
        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);

        // Iterate to update augmentedImageMap, remove elements we cannot draw.
        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            switch (augmentedImage.getTrackingState()) {
                case PAUSED:
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                    String text = "Detected Image Paused " + augmentedImage.getName();
                    System.out.println(text);
                    messageSnackbarHelper.showMessage(this, text);
                    break;

                case TRACKING:
                    // Have to switch to UI Thread to update View.
                    this.runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    fitToScanView.setVisibility(View.GONE);
                                }
                            });

                    // Create a new anchor for newly found images.
                    if (!augmentedImageMap.containsKey(augmentedImage.getIndex())) {
                        Anchor centerPoseAnchor = augmentedImage.createAnchor(augmentedImage.getCenterPose());
                        augmentedImageMap.put(
                                augmentedImage.getIndex(), Pair.create(augmentedImage, centerPoseAnchor));
                    }
                    String text1 = "Detected Image Tracking " + augmentedImage.getName();
                    System.out.println(text1);
                    messageSnackbarHelper.showMessage(this, text1);
                    break;

                case STOPPED:
                    augmentedImageMap.remove(augmentedImage.getIndex());
                    String text2 = "Detected Image Stop " + augmentedImage.getName();
                    System.out.println(text2);
                    messageSnackbarHelper.showMessage(this, text2);
                    break;

                default:
                    break;
            }
        }

        // Draw all images in augmentedImageMap
        for (Pair<AugmentedImage, Anchor> pair : augmentedImageMap.values()) {
            AugmentedImage augmentedImage = pair.first;
            Anchor centerAnchor = augmentedImageMap.get(augmentedImage.getIndex()).second;

            switch (augmentedImage.getTrackingState()) {
                case TRACKING:
                    augmentedImageRenderer.draw(
                            viewmtx, projmtx, augmentedImage, centerAnchor, colorCorrectionRgba);
                    break;
                default:
                    break;
            }
        }
    }*/

    /*private final BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if(status == LoaderCallbackInterface.SUCCESS){

                System.out.println("Hallo");
            }else{
                super.onManagerConnected(status);
            }

        }
    };*/

    /*
     private void recognizeRectangle(int[] intMap, int width, int height) {

        takePic = false;
        System.out.println("Hello");
        ArrayList<PContour.Contour> contours = new PContour().findContours(intMap, width, height);

        System.out.println("Size: " + contours.size());
        /*Bitmap bitmap = ImageConverter.JPGtoRGB888(jpgBitmap);

        Mat blackWhite = new Mat();
        Mat downscaled = new Mat();
        //Mat hueSaturationValue = new Mat();
        Mat lowerRedRange = new Mat();
        Mat upperRedRange = new Mat();
        Mat upscaled = new Mat();
        Mat contour = new Mat();
        Mat hierarchyOutputVector = new Mat();
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        int threshold = 100;

        //TODO hier bestimmt was geht was schief
        //Mat gray = new Mat(height, width, CvType.CV_8UC1);
        //Mat dst = new Mat(height, width, CvType.CV_8UC1);

        Mat gray = new Mat();
        Mat dst = new Mat();

        org.opencv.android.Utils.bitmapToMat(toGrayscale(bitmap), gray);
        org.opencv.android.Utils.bitmapToMat(bitmap, dst);

        Imgproc.pyrDown(gray, downscaled, new Size(gray.cols() / 2, gray.rows() / 2));
        Imgproc.pyrUp(downscaled, upscaled, gray.size());

        Imgproc.Canny(upscaled, blackWhite, 0, threshold);

        Imgproc.dilate(blackWhite, blackWhite, new Mat(), new org.opencv.core.Point(-1, 1), 1);

        List<MatOfPoint> contours = new ArrayList<>();

        contour = blackWhite.clone();

        Imgproc.findContours(contour, contours, hierarchyOutputVector, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        System.out.println("Hello1");
    int counter = 0;

        for (PContour.Contour cnt : contours) {

        //MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());
        ArrayList<PContour.Point> points = new PContour().approxPolyDP(cnt.points, 1f);

        //Imgproc.approxPolyDP(curve, approxCurve, 0.02 * Imgproc.arcLength(curve, true), true);

        int numberVertices = points.size();

        System.out.println("vertices: " + numberVertices);

        // int numberVertices = (int) approxCurve.total();


            double contourArea = Imgproc.contourArea(cnt);

            if (Math.abs(contourArea) < 100) {
                continue;
            }

        //Rectangle detected
        if (numberVertices == 4) {

            List<Double> cos = new ArrayList<>();

            for (int j = 2; j < numberVertices + 1; j++) {
                cos.add(angle(points.get(j % numberVertices), points.get(j - 2), points.get(j - 1)));
            }

            Collections.sort(cos);

            double mincos = cos.get(0);
            double maxcos = cos.get(cos.size() - 1);

            if (numberVertices == 4 && mincos >= -0.1 && maxcos <= 0.3) {

                System.out.println("Hello again, Ich sag einfach nur hello again.");
                handleRectangleDetection(cnt, counter);
                counter++;
            }
        }
    }
        System.out.println("So many Rectangles detected: "+ counter);
}
     */

/*
    private boolean generateUserInstructions(Camera camera) {
        String message = null;
        if (camera.getTrackingState() == TrackingState.PAUSED) {
            if (camera.getTrackingFailureReason() == TrackingFailureReason.NONE) {
                message = SEARCHING_PLANE_MESSAGE;
            } else {
                message = TrackingStateHelper.getTrackingFailureReasonString(camera);
            }
        } else if (hasTrackingPlane()) {
            if (wrappedAnchor == null) {
                message = NO_SIGN_FOUND_MESSAGE;
            }
        } else {
            message = SEARCHING_PLANE_MESSAGE;
        }
        if (message == null) {
            messageSnackbarHelper.hide(this);
            return true;
        } else {
            messageSnackbarHelper.showMessage(this, message);
            return false;
        }
    }*/


    /* private void recognizeRectangle(int[] intMap, int width, int height) {

        takePic = false;
        System.out.println("Hello");
        ArrayList<PContour.Contour> contours = new PContour().findContours(intMap, width, height);

        System.out.println("Size: " + contours.size());

        /*Bitmap bitmap = ImageConverter.JPGtoRGB888(jpgBitmap);

        Mat blackWhite = new Mat();
        Mat downscaled = new Mat();
        //Mat hueSaturationValue = new Mat();
        Mat lowerRedRange = new Mat();
        Mat upperRedRange = new Mat();
        Mat upscaled = new Mat();
        Mat contour = new Mat();
        Mat hierarchyOutputVector = new Mat();
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        int threshold = 100;

        //TODO hier bestimmt was geht was schief
        //Mat gray = new Mat(height, width, CvType.CV_8UC1);
        //Mat dst = new Mat(height, width, CvType.CV_8UC1);

        Mat gray = new Mat();
        Mat dst = new Mat();

        org.opencv.android.Utils.bitmapToMat(toGrayscale(bitmap), gray);
        org.opencv.android.Utils.bitmapToMat(bitmap, dst);

        Imgproc.pyrDown(gray, downscaled, new Size(gray.cols() / 2, gray.rows() / 2));
        Imgproc.pyrUp(downscaled, upscaled, gray.size());

        Imgproc.Canny(upscaled, blackWhite, 0, threshold);

        Imgproc.dilate(blackWhite, blackWhite, new Mat(), new org.opencv.core.Point(-1, 1), 1);

        List<MatOfPoint> contours = new ArrayList<>();

        contour = blackWhite.clone();

        Imgproc.findContours(contour, contours, hierarchyOutputVector, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);*/
/*
        System.out.println("Hello1");
    int counter = 0;

        for (PContour.Contour cnt : contours) {

        //MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());
        ArrayList<PContour.Point> points = new PContour().approxPolyDP(cnt.points, 1f);

        //Imgproc.approxPolyDP(curve, approxCurve, 0.02 * Imgproc.arcLength(curve, true), true);

        int numberVertices = points.size();

        System.out.println("vertices: " + numberVertices);

        //Rectangle detected
        if (numberVertices == 4) {

            List<Double> cos = new ArrayList<>();

            for (int j = 2; j < numberVertices + 1; j++) {
                cos.add(angle(points.get(j % numberVertices), points.get(j - 2), points.get(j - 1)));
            }

            Collections.sort(cos);

            double mincos = cos.get(0);
            double maxcos = cos.get(cos.size() - 1);

            if (numberVertices == 4 && mincos >= -0.1 && maxcos <= 0.3) {

                System.out.println("Hello again, Ich sag einfach nur hello again.");
                handleRectangleDetection(cnt, counter);
                counter++;
            }
        }
    }
        System.out.println("So many Rectangles detected: "+ counter);
}
 public void handleRectangleDetection(PContour.Contour cnt, int counter) {
        //org.opencv.core.Rect r = Imgproc.boundingRect(cnt);
        //org.opencv.core.Point pt = new org.opencv.core.Point(r.x + ((r.width /*- text.width) / 2),r.y + ((r.height /* + text.height) / 2));

    writeToFile("\n\nRectangle " + counter + ":\n X0:"  + cnt.points.get(0).x + " Y0:" + cnt.points.get(0).y + "\n X1:"  + cnt.points.get(1).x + " Y1:" + cnt.points.get(1).y + "\n X2:"  + cnt.points.get(2).x + " Y2:" + cnt.points.get(2).y + "\n X3:"  + cnt.points.get(3).x + " Y3:" + cnt.points.get(3).y);
}
private static double angle(PContour.Point pt1, PContour.Point pt2, PContour.Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }


////////////////////
        byte[] testData = new byte[(int) (thresh.total() * thresh.channels())];
        thresh.get(0, 0, testData);
        byte[] newTestData = new byte[(int) (thresh.total() * thresh.channels())];

        for (int y = 0; y < thresh.rows(); y++) {
            for (int x = 0; x < thresh.cols(); x++) {
                for (int c = 0; c < thresh.channels(); c++) {
                    int pixelValue = testData[(y * thresh.cols() + x) * thresh.channels() + c];
                    if (pixelValue == 0) {
                        newTestData[(y * thresh.cols() + x) * thresh.channels() + c] = 0;
                    } else {
                        newTestData[(y * thresh.cols() + x) * thresh.channels() + c] = Integer.valueOf(255).byteValue();
                    }
                }
            }
        }


        //TODO speicher Allokieren
        Mat tempThresImg1 = new Mat();
        //tempThresImg1.put(0, 0, newTestData);
        cvtColor(thresh, tempThresImg1,COLOR_GRAY2RGB);
        Imgcodecs.imwrite(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/HelloAR/" + "Test" + Long.toHexString(System.currentTimeMillis()) + ".png", tempThresImg1);

//////////////////////////
 List<Integer> nonZeroValues = new ArrayList<>();

        byte[] imageData = new byte[(int) (blur.total() * blur.channels())];
        blur.get(0, 0, imageData);

        for (int y = 0; y < blur.rows(); y++) {
            for (int x = 0; x < blur.cols(); x++) {
                for (int c = 0; c < blur.channels(); c++) {
                    int pixelValue = imageData[(y * blur.cols() + x) * blur.channels() + c];
                    if (pixelValue > 0) {
                        nonZeroValues.add(pixelValue);
                    }
                }
            }
        }
        byte[] newImageData = new byte[(int) (nonZeroValues.size())];

        Iterator<Integer> iterator = nonZeroValues.iterator();
        for (int i = 0; i < nonZeroValues.size(); i++) {
            newImageData[i] = iterator.next().byteValue();
        }

        //TODO speicher Allokieren
        Mat tempThresImg = new Mat();
        tempThresImg.put(0, 0, newImageData);





 */



}
