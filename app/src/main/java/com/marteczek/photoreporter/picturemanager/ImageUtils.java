package com.marteczek.photoreporter.picturemanager;

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
}
