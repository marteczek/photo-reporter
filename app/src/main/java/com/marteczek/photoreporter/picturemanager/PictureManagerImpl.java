package com.marteczek.photoreporter.picturemanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;
import androidx.lifecycle.MutableLiveData;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.marteczek.photoreporter.application.Settings.Debug.E;
import static com.marteczek.photoreporter.picturemanager.ImageUtils.calculateInSampleSize;


public class PictureManagerImpl implements PictureManager {
    private static final String TAG = "PictureManagerImpl";

    private static final String URL_STATIC_MAP = "https://maps.googleapis.com/maps/api/staticmap";

    private final static int MAP_SIZE = 200;

    private final static int MAP_MARGIN = 20;

    private final Context context;

    public PictureManagerImpl(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String copy(String sourceUri, String newFileName) throws IOException {
        Uri uri = Uri.parse(sourceUri);
        InputStream in = context.getContentResolver().openInputStream(uri);
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
        Bitmap bitmap = prepareBaseBitmap(sourcePath, rotation, greaterDimension);
        if (Settings.shouldApplyWatermark(context)) {
            applyWaterMark(bitmap);
        }
        if (Settings.shouldApplyMap(context)) {
            downloadAndApplyMap(sourcePath, bitmap);
        }
        return bitmap;
    }

    @Override
    public void preparePictureForUpload(String sourcePath, int rotation, int greaterDimension,
                                        MutableLiveData<Bitmap> resultantBitmap) {
        Bitmap bitmap = prepareBaseBitmap(sourcePath, rotation, greaterDimension);
        if (Settings.shouldApplyWatermark(context)) {
            applyWaterMark(bitmap);
        }
        resultantBitmap.postValue(bitmap);
        if (Settings.shouldApplyMap(context)) {
            downloadAndApplyMap(sourcePath, bitmap);
            resultantBitmap.postValue(bitmap);
        }
    }

    private Bitmap prepareBaseBitmap(String sourcePath, int rotation, float greaterDimension) {
        Bitmap bitmap = BitmapFactory.decodeFile(sourcePath);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        float scale = greaterDimension / Math.max(width, height);
        matrix.postScale(scale, scale);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        return bitmap;
    }

    private void applyWaterMark(Bitmap bitmap) {
        int width;
        int height;
        width = bitmap.getWidth();
        height = bitmap.getHeight();
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

    private void downloadAndApplyMap(String sourcePath, Bitmap baseBitmap) {
        double[] latLong;
        double angleOfView = 90;
        Double imageDirection;
        try {
            ExifInterface exif = new androidx.exifinterface.media.ExifInterface(sourcePath);
            latLong = exif.getLatLong();
            if (latLong == null) {
                return;
            }
            double focalLength = exif.getAttributeDouble("FocalLengthIn35mmFilm", -1);
            if (focalLength != -1) {
                int filmSize = 35;
                angleOfView = 2 * Math.atan(filmSize / (2 * focalLength)) * 180 / Math.PI;
            }
            imageDirection = exif.getAttributeDouble("GPSImgDirection", -1);
            if (imageDirection == -1) {
                imageDirection = null;
            }
        } catch (IOException e) {
            return;
        }
        int zoomDifference = Settings.getMapZoom(context);
        boolean addMarker = !Settings.shouldApplyGPSDirection(context) || imageDirection == null;
        String shape = Settings.getMapShape(context);
        int copyrightHeight = 25;
        int additionalHeight = shape.equals(context.getString(R.string.preference_circle))
                ? copyrightHeight : 0;
        String url = URL_STATIC_MAP +
                "?center=" + latLong[0] + "," + latLong[1] +
                "&size=" + MAP_SIZE + "x" + (MAP_SIZE + additionalHeight) +
                "&zoom=" + (16 + zoomDifference) +
                (addMarker ? "&markers=" + latLong[0] + "," + latLong[1] : "") +
                "&key=" + Settings.getGoogleStaticMapKey();
        OkHttpClient client = new OkHttpClient();
        Request loadMapRequest = new Request.Builder().url(url).build();
        Bitmap mapBitmap;
        try {
            Response response = client.newCall(loadMapRequest).execute();
            InputStream stream = response.body().byteStream();
            mapBitmap = BitmapFactory.decodeStream(stream);
        } catch (IOException e) {
            return;
        }
        if (mapBitmap != null) {
            Bitmap bitmap = Bitmap.createBitmap(MAP_SIZE, MAP_SIZE + additionalHeight,
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            if (shape.equals(context.getString(R.string.preference_circle))) {
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setColor(0XFF000000);
                canvas.drawCircle(MAP_SIZE / 2, MAP_SIZE / 2, MAP_SIZE / 2, paint);
                if (additionalHeight > 0) {
                    canvas.drawRect(0, MAP_SIZE, MAP_SIZE - 1, MAP_SIZE - 1 + additionalHeight, paint);
                }
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                canvas.drawBitmap(mapBitmap, 0, 0, paint);
            } else {
                canvas.drawBitmap(mapBitmap,0,0, null);
            }
            if (Settings.shouldApplyGPSDirection(context) && imageDirection != null) {
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setColor(0X40000000);
                int padding = 10;
                canvas.drawArc(new RectF(padding, padding, MAP_SIZE - 1 - padding, MAP_SIZE - 1 - padding),
                        -90 + imageDirection.floatValue() - (float) angleOfView / 2,
                        (float) angleOfView, true, paint);
                paint.setColor(0XFF800000);
                canvas.drawCircle(MAP_SIZE /2, MAP_SIZE /2, 3, paint);
            }
            int left = baseBitmap.getWidth() - MAP_MARGIN - MAP_SIZE;
            int top = MAP_MARGIN;
            Paint paint = new Paint();
            paint.setAlpha(255 - (int) (2.55 * Settings.getMapTransparency(context)));
            canvas = new Canvas(baseBitmap);
            canvas.drawBitmap(bitmap, left, top, paint);
        }
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

    @Override
    public int getImageRotation(String path) {
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