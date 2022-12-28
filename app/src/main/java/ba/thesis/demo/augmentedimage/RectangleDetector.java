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

    private byte saturate(double val) {
        int iVal = (int) Math.round(val);
        iVal = iVal > 255 ? 255 : (Math.max(iVal, 0));
        return (byte) iVal;
    }

    public Point detectRectangle(Bitmap image, CenterPoint planetCenter) {
        Mat source = new Mat();
        Utils.bitmapToMat(image, source);

        Mat gray = new Mat();
        Mat blur = new Mat();
        Mat th3 = new Mat();
        Mat ero = new Mat();
        Mat temp = new Mat();

        //Gamma Manipulation
        Mat lookUpTable = new Mat(1, 256, CvType.CV_8U);
        double gammaValue= 0.8;
        byte[] lookUpTableData = new byte[(int) (lookUpTable.total()*lookUpTable.channels())];
        for (int i = 0; i < lookUpTable.cols(); i++) {
            lookUpTableData[i] = saturate(Math.pow(i / 255.0, gammaValue) * 255.0);
        }
        lookUpTable.put(0, 0, lookUpTableData);
        Mat img = new Mat();
        Core.LUT(source, lookUpTable, img);

        //TODO hier fehlt Thresh
        Mat dst = new Mat();
        threshold(img, dst, 177, 200, THRESH_BINARY);

        //convert to gray scale
        cvtColor(dst, gray, COLOR_BGR2GRAY);
        medianBlur(gray, blur, 5);

        //thresholding to find only white region of image, without black backgorund
        threshold(blur, th3, 0, 255, THRESH_BINARY + THRESH_OTSU);

        //erosion to delete some noises
        //TODO
        Mat kernel = Mat.ones(5, 5, CvType.CV_8U);
        erode(th3, ero, kernel,  new Point(-1,-1), 1);

        //get threshold value only for not black pixels
        //double thresholdValue = threshold(tempThresImg, temp, 0, 255, THRESH_BINARY + THRESH_OTSU);
        //using otsu threshold

        Mat thresh = new Mat();

        //use threshold value on whole image
        threshold(blur, thresh, 20, 255, THRESH_BINARY);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchyOutputVector = new Mat();

        //System.out.println("ThresholdValue: " + thresholdValue);
        Imgproc.findContours(thresh, contours, hierarchyOutputVector, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // paint whole contour
        drawContours(thresh, contours, -1, new Scalar(255,255,255), -1);

        // Morph open
        Mat kernel2 = getStructuringElement(MORPH_RECT, new Size(9,9));
        Mat opening = new Mat();

        morphologyEx(thresh, opening, MORPH_OPEN, kernel2, new Point(-1,-1), 4);


        //imgCanny = Canny(opening, 137, 200);

        List<MatOfPoint> contours2 = new ArrayList<>();
        //Draw rectangles, the 'area_threshold' value was determined empirically
        Imgproc.findContours(opening, contours2, hierarchyOutputVector,  Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        //contours2 = contours2.size() == 2 ? contours2.get(0) : contours2.get(1);
        int area_threshold = 4000;
        for (MatOfPoint c : contours2) {
            MatOfPoint2f curve = new MatOfPoint2f(c.toArray());
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(curve, approxCurve, 0.02 * Imgproc.arcLength(curve, true), true);
            int numberVertices = (int) approxCurve.total();

            if (contourArea(c) > area_threshold && numberVertices >= 4 && numberVertices <= 6) {
                //drawContours(source, contours, b, new Scalar(255,255,255), 15);

                Rect rect = Imgproc.boundingRect(c);

                if (rect.contains(new Point(planetCenter.getX(), planetCenter.getY()))) {
                    Point pt = new Point(rect.x + ((float)(rect.width) / 2),rect.y + ((float)(rect.height) / 2));

                    setLabel(source, "X", c);
                    setLabel(source, "y", planetCenter);
                    setLabel(source, "O", new CenterPoint((float) (planetCenter.getX() + pt.x) / 2,
                            (float) (planetCenter.getY() + pt.y) / 2));

                    //rectangle(source, rect, new Scalar(36, 255, 12));
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
