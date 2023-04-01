package ba.thesis.demo.augmentedimage;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.MORPH_OPEN;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.medianBlur;
import static org.opencv.imgproc.Imgproc.morphologyEx;
import static org.opencv.imgproc.Imgproc.threshold;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * This class uses OpenCV for rectangleDetection
 */
public class RectangleDetector {

    public Point detectRectangle(Bitmap image, CenterPoint planetCenter) {
        Mat source = new Mat();
        Utils.bitmapToMat(image, source);

        Mat gray = new Mat();
        Mat blur = new Mat();

        // convert image to grayscale
        cvtColor(source, gray, COLOR_BGR2GRAY);

        Mat threshold = new Mat();

        // Otsu threshold on grayscale image
        threshold(gray, threshold, 0, 255, THRESH_OTSU);

        // median filter
        medianBlur(threshold, blur, 5);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchyOutputVector = new Mat();

        // findContours algorithm
        Imgproc.findContours(blur, contours, hierarchyOutputVector, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // paint contours to remove regions with big holes
        drawContours(blur, contours, -1, new Scalar(255, 255, 255), -1);

        // rectangular window for opening filter
        Mat kernel = getStructuringElement(MORPH_RECT, new Size(18, 18));
        Mat opening = new Mat();

        // Opening filter
        morphologyEx(blur, opening, MORPH_OPEN, kernel, new Point(-1, -1), 8);

        List<MatOfPoint> contours2 = new ArrayList<>();

        // findContours algorithm
        Imgproc.findContours(opening, contours2, hierarchyOutputVector, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        int area_threshold = 40000;
        for (MatOfPoint c : contours2) {

            MatOfPoint2f curve = new MatOfPoint2f(c.toArray());
            MatOfPoint2f approxCurve = new MatOfPoint2f();

            // lineApproximation
            Imgproc.approxPolyDP(curve, approxCurve, 0.02 * Imgproc.arcLength(curve, true), true);
            int numberVertices = (int) approxCurve.total();

            // check if area of contour is bigger than the area_threshold and has 4 - 5 vertices
            if (contourArea(c) > area_threshold && numberVertices >= 4 && numberVertices <= 5) {
                Rect rect = Imgproc.boundingRect(c);

                if (rect.contains(new Point(planetCenter.getX(), planetCenter.getY()))) {
                    // center of rectangle
                    return new Point(rect.x + ((float) (rect.width) / 2), rect.y + ((float) (rect.height) / 2));
                }
            }
        }
        return null;
    }
}
