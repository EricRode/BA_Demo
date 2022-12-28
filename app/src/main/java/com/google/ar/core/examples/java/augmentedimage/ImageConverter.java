package com.google.ar.core.examples.java.augmentedimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageConverter {

    public static Bitmap getBitmap(Image image) throws IOException {
        int imageFormat = image.getFormat();
        if (imageFormat == ImageFormat.YUV_420_888) {
            Log.d("ImageFormat", "Image format is YUV_420_888");
        }
        byte[] jpeg = toJpegImage(image);

        Bitmap bitmapImage = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);

        final File out = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/HelloAR", "Img" + Long.toHexString(System.currentTimeMillis()) + ".png");

        // Write it to disk. Delete later.
        FileOutputStream fos = new FileOutputStream(out);
        bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.flush();
        fos.close();
        return bitmapImage;
    }

    // TODO Quelle
    public static byte[] toJpegImage(Image image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Invalid image format");
        }

        YuvImage yuvImage = toYuvImage(image);
        int width = image.getWidth();
        int height = image.getHeight();

        // Convert to jpeg
        byte[] jpegImage = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        jpegImage = out.toByteArray();

        return jpegImage;
    }

    public static Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public static Bitmap JPGtoRGB888(Bitmap img){
        Bitmap result = null;

        int numPixels = img.getWidth() * img.getHeight();
        int[] pixels = new int[numPixels];

//        get jpeg pixels, each int is the color value of one pixel
        img.getPixels(pixels,0,img.getWidth(),0,0,img.getWidth(),img.getHeight());

//        create bitmap in appropriate format
        result = Bitmap.createBitmap(img.getWidth(),img.getHeight(), Bitmap.Config.ARGB_8888);

//        Set RGB pixels
        result.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());

        return result;
    }

    // TODO Quelle
    public static YuvImage toYuvImage(Image image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Invalid image format");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // Order of U/V channel guaranteed, read more:
        // https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888
        Image.Plane yPlane = image.getPlanes()[0];
        Image.Plane uPlane = image.getPlanes()[1];
        Image.Plane vPlane = image.getPlanes()[2];

        ByteBuffer yBuffer = yPlane.getBuffer();
        ByteBuffer uBuffer = uPlane.getBuffer();
        ByteBuffer vBuffer = vPlane.getBuffer();

        // Full size Y channel and quarter size U+V channels.
        int numPixels = (int) (width * height * 1.5f);
        byte[] nv21 = new byte[numPixels];
        int index = 0;

        // Copy Y channel.
        int yRowStride = yPlane.getRowStride();
        int yPixelStride = yPlane.getPixelStride();
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                nv21[index++] = yBuffer.get(y * yRowStride + x * yPixelStride);
            }
        }

        // Copy VU data; NV21 format is expected to have YYYYVU packaging.
        // The U/V planes are guaranteed to have the same row stride and pixel stride.
        int uvRowStride = uPlane.getRowStride();
        int uvPixelStride = uPlane.getPixelStride();
        int uvWidth = width / 2;
        int uvHeight = height / 2;

        for (int y = 0; y < uvHeight; ++y) {
            for (int x = 0; x < uvWidth; ++x) {
                int bufferIndex = (y * uvRowStride) + (x * uvPixelStride);
                // V channel.
                nv21[index++] = vBuffer.get(bufferIndex);
                // U channel.
                nv21[index++] = uBuffer.get(bufferIndex);
            }
        }
        return new YuvImage(
                nv21, ImageFormat.NV21, width, height, /* strides= */ null);
    }
}
