package com.marteczek.photoreporter.picturemanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.marteczek.photoreporter.application.Settings.Debug.E;
import static com.marteczek.photoreporter.imagetools.ImageUtils.calculateInSampleSize;


public class PictureManagerImpl implements PictureManager {
    private static final String TAG = "PictureManager";

    private Context context;

    public PictureManagerImpl(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String copy(Uri sourceUri, String newFileName) throws IOException {
        InputStream in = context.getContentResolver().openInputStream(sourceUri);
        File outFile = new File(getFilesDir(), newFileName);
        OutputStream out = new FileOutputStream(outFile);
        byte[] buffer = new byte[1024];
        int lengthRead;
        while ((lengthRead = in.read(buffer)) > 0) {
            out.write(buffer, 0, lengthRead);
            out.flush();
        }
        return outFile.getAbsolutePath();
    }

    @Override
    public File getFilesDir() {
        return context.getFilesDir();
    }

    @Override
    public String generateThumbnail(String sourcePath, String newFileName,
                                    int requiredWidth, int requiredHeight) {
        return resizeImage(sourcePath, newFileName,requiredWidth, requiredHeight);
    }

    @Override
    public String resizeImage(String sourcePath, String newFileName,
                              int requiredWidth, int requiredHeight) {
        String destinationPath = new File(getFilesDir(), newFileName).getAbsolutePath();
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(sourcePath, options);
        options.inSampleSize = calculateInSampleSize(options.outHeight, options.outWidth,
                requiredWidth, requiredHeight);
        options.inJustDecodeBounds = false;
        final Bitmap bitmap = BitmapFactory.decodeFile(sourcePath, options);
        try (FileOutputStream out = new FileOutputStream(destinationPath)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            if(E) Log.e(TAG, "IOException", e);
            destinationPath = null;
        }
        return destinationPath;
    }

    @Override
    public Bitmap preparePictureForUpload(String sourcePath, int rotation, int greaterDimension) {
        Bitmap bitmap = BitmapFactory.decodeFile(sourcePath);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        float scale = (float) greaterDimension / Math.max(width, height);
        matrix.postScale(scale, scale);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        if (Settings.shouldApplyWatermark(context)) {
            String watermarkFileName = new File(getFilesDir(), Settings.WATERMARK_FILE_NAME).getAbsolutePath();
            Bitmap watermark = BitmapFactory.decodeFile(watermarkFileName);
            if (watermark != null) {
                int left = 0;
                int top = 0;
                int distance = Settings.getWatermarkDistance(context);
                String position = Settings.getWatermarkPosition(context);
                if (position.equals(context.getString(R.string.preference_top_left))) {
                    left = distance;
                    top = distance;
                }
                if (position.equals(context.getString(R.string.preference_top_right))) {
                    left = width - watermark.getWidth() - distance;
                    top = distance;
                }
                if (position.equals(context.getString(R.string.preference_bottom_left))) {
                    left = distance;
                    top = height - watermark.getHeight() - distance;
                }
                if (position.equals(context.getString(R.string.preference_bottom_right))) {
                    left = width - watermark.getWidth() - distance;
                    top = height - watermark.getHeight() - distance;
                }
                Canvas canvas = new Canvas(bitmap);
                canvas.drawBitmap(watermark, left, top, null);
            }
        }
        return bitmap;
    }

    @Override
    public String savePicture(Bitmap bitmap, String newFileName) {
        String destinationPath = new File(getFilesDir(), newFileName).getAbsolutePath();
        try (FileOutputStream out = new FileOutputStream(destinationPath)) {
            int quality = Settings.getPictureQuality(context);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        } catch (IOException e) {
            if(E) Log.e(TAG, "IOException", e);
            destinationPath = null;
        }
        return destinationPath;
    }

    @Override
    public boolean deleteFile(String path) {
        File file = new File(path);
        return file.delete();
    }
}
