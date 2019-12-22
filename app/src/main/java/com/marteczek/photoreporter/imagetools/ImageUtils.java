package com.marteczek.photoreporter.imagetools;

import android.media.ExifInterface;

import java.io.IOException;

public class ImageUtils {

    public static int calculateInSampleSize(int height, int width,
                                            int requiredWidth, int requiredHeight) {
        int inSampleSize = 1;
        if (height > requiredHeight || width > requiredWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= requiredHeight
                    && (halfWidth / inSampleSize) >= requiredWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static int getImageRotation(String path) {
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
            }
        } catch (IOException e) {
            //ignored
        }
        return 0;
    }
}
