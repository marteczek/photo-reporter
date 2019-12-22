package com.marteczek.photoreporter.picturemanager;

import android.net.Uri;

import java.io.IOException;

public interface PictureManager {
    String copy(Uri sourceUri, String newFileName) throws IOException;

    String generateThumbnail(String sourcePath, String newFileName,
                             int requiredWidth, int requiredHeight);

    String resizeImage(String sourcePath, String newFileName,
                       int requiredWidth, int requiredHeight);

    String rotateAndResizePicture(String sourcePath, String newFileName,
                                  int pictureRotation, int greaterDimension);

    boolean deleteFile(String path);
}
