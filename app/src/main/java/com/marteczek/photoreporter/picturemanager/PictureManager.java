package com.marteczek.photoreporter.picturemanager;

import android.graphics.Bitmap;

import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.IOException;

public interface PictureManager {
    String copy(String sourceUri, String newFileName) throws IOException;

    File getFilesDir();

    String generateThumbnail(String sourcePath, String newFileName,
                             int requiredWidth, int requiredHeight);

    String resizeImage(String sourcePath, String newFileName,
                       int requiredWidth, int requiredHeight);

    Bitmap preparePictureForUpload(String sourcePath, int rotation, int greaterDimension);

    void preparePictureForUpload(String sourcePath, int rotation, int greaterDimension,
                                 MutableLiveData<Bitmap> resultantBitmap);

    String savePicture(Bitmap bitmap, String newFileName);

    boolean deleteFile(String path);

    int getImageRotation(String path);
}
