package ba.thesis.demo.augmentedimage;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.MORPH_OPEN;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;
import static org.opencv.imgproc.Imgproc.approxPolyDP;
import static org.opencv.imgproc.Imgproc.arcLength;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.medianBlur;
import static org.opencv.imgproc.Imgproc.morphologyEx;
import static org.opencv.imgproc.Imgproc.rectangle;
import static org.opencv.imgproc.Imgproc.threshold;

import android.graphics.Bitmap;
import android.os.Environment;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;


public class RectangleDetector {

    public Point detectRectangle(Bitmap image, CenterPoint planetCenter) {
        Mat source = new Mat();
        Utils.bitmapToMat(image, source);

        Mat gray = new Mat();
        Mat blur = new Mat();
        //Mat gammaCorrection = new Mat();

        cvtColor(source, gray, COLOR_BGR2GRAY);

       /* //Gamma Manipulation
        Mat lookUpTable = new Mat(1, 256, CvType.CV_8U);
        double gammaValue = 2.0;
        byte[] lookUpTableData = new byte[(int) (lookUpTable.total()*lookUpTable.channels())];
        for (int i = 0; i < lookUpTable.cols(); i++) {
            lookUpTableData[i] = saturate(Math.pow(i / 255.0, gammaValue) * 255.0);
        }
        lookUpTable.put(0, 0, lookUpTableData);
        Core.LUT(gray, lookUpTable, gammaCorrection);*/

        Mat threshold = new Mat();
        threshold(gray, threshold, 0, 255, THRESH_OTSU);

        //convert to gray scale
        medianBlur(threshold, blur, 5);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchyOutputVector = new Mat();

        //System.out.println("ThresholdValue: " + thresholdValue);
        Imgproc.findContours(blur, contours, hierarchyOutputVector, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // paint whole contour
        drawContours(blur, contours, -1, new Scalar(255,255,255), -1);

        // Morph open
        //TODO nachprüfen ob 18 nicht zu groß ist
        Mat kernel = getStructuringElement(MORPH_RECT, new Size(18,18));
        Mat opening = new Mat();

        morphologyEx(blur, opening, MORPH_OPEN, kernel, new Point(-1,-1), 8);

        List<MatOfPoint> contours2 = new ArrayList<>();
        Imgproc.findContours(opening, contours2, hierarchyOutputVector,  Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        int area_threshold = 40000;
        for (MatOfPoint c : contours2) {
            MatOfPoint2f curve = new MatOfPoint2f(c.toArray());
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(curve, approxCurve, 0.02 * Imgproc.arcLength(curve, true), true);
            int numberVertices = (int) approxCurve.total();

            if (contourArea(c) > area_threshold && numberVertices >= 4 && numberVertices <= 5) {

                Rect rect = Imgproc.boundingRect(c);

                if (rect.contains(new Point(planetCenter.getX(), planetCenter.getY()))) {
                    Point pt = new Point(rect.x + ((float)(rect.width) / 2),rect.y + ((float)(rect.height) / 2));

                    //setLabel(source, "X", c);
                    //setLabel(source, "D", planetCenter);
                    //setLabel(source, "O", new CenterPoint((float) (planetCenter.getX() + pt.x) / 2,(float) (planetCenter.getY() + pt.y) / 2));

                    //rectangle(source, rect, new Scalar(255, 255, 255));
                    //Imgcodecs.imwrite(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/HelloAR/" + "Rectangle" + Long.toHexString(System.currentTimeMillis()) + ".png", source);
                    return pt;
                }
            }
        }
        return null;
    }

    private void setLabel(Mat im, String label, MatOfPoint contour) {
        int fontface = 0;
        double scale = 1;//0.4;
        int thickness = 3;//1;
        int[] baseline = new int[1];
        Size text = Imgproc.getTextSize(label, fontface, scale, thickness, baseline);
        Rect r = Imgproc.boundingRect(contour);
        Point pt = new Point(r.x + ((r.width - text.width) / 2),r.y + ((r.height + text.height) / 2));
        Imgproc.putText(im, label, pt, fontface, scale, new Scalar(255, 0, 0), thickness);
    }

    private void setLabel(Mat im, String label, CenterPoint centerPoint) {
        int fontface = 0;
        double scale = 1;//0.4;
        int thickness = 3;//1;
        int[] baseline = new int[1];
        Size text = Imgproc.getTextSize(label, fontface, scale, thickness, baseline);
        Point pt = new Point(centerPoint.getX(), centerPoint.getY());
        Imgproc.putText(im, label, pt, fontface, scale, new Scalar(255, 0, 0), thickness);
    }

}
