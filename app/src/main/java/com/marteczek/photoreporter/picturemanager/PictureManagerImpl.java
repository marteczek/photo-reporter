package com.marteczek.photoreporter.picturemanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
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

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.marteczek.photoreporter.application.Settings.Debug.D;
import static com.marteczek.photoreporter.application.Settings.Debug.E;
import static com.marteczek.photoreporter.picturemanager.ImageUtils.calculateInSampleSize;


public class PictureManagerImpl implements PictureManager {
    private static final String TAG = "PictureManagerImpl";

    private static final String URL_GOOGLE_STATIC_MAP = "https://maps.googleapis.com/maps/api/staticmap";

    private static final String USER_AGENT_FOR_OSM = "PhotoReporter/1.0 https://github.com/marteczek/photo-reporter";

    private static final int CACHE_SIZE = 32 * 1024 * 1024;

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
        ExifData exifData = readExifData(sourcePath);
        double[] latLong = exifData.latLong;
        Double shuttingDirection = exifData.shuttingDirection;
        Double focalLength = exifData.focalLength;
        double angleOfView = 90;
        if (focalLength != null) {
            int filmSize = 35;
            angleOfView = 2 * Math.atan(filmSize / (2 * focalLength)) * 180 / Math.PI;
        }
        int baseZoom = 16;
        int zoomDifference = Settings.getMapZoom(context);
        int zoom = baseZoom + zoomDifference;
        boolean addMarker = !Settings.shouldApplyGPSDirection(context) || shuttingDirection == null;
        OkHttpClient client = new OkHttpClient.Builder()
                .cache( new Cache(context.getCacheDir(), CACHE_SIZE))
                .build();
        if (context.getString(R.string.preference_google).equals(Settings.getMapProvider(context))) {
            Bitmap mapBitmap = downloadGoogleMap(client, latLong, zoom, addMarker);
            if (mapBitmap != null) {
                drawGoogleMap(baseBitmap, (float) angleOfView, shuttingDirection, mapBitmap);
            }
        }
        if (context.getString(R.string.preference_osm).equals(Settings.getMapProvider(context))) {
            Bitmap mapBitmap = downloadOsmMap(client, latLong, zoom);
            if (mapBitmap != null) {
                drawOsmMap(baseBitmap, (float) angleOfView, shuttingDirection, mapBitmap);
            }
        }
    }

    private class ExifData {
        double[] latLong;
        Double shuttingDirection;
        Double focalLength;

        ExifData(double[] latLong, Double shuttingDirection, Double focalLength) {
            this.latLong = latLong;
            this.shuttingDirection = shuttingDirection;
            this.focalLength = focalLength;
        }
    }

    private ExifData readExifData(String sourcePath) {
        try {
            ExifInterface exif = new androidx.exifinterface.media.ExifInterface(sourcePath);
            double[] latLong = exif.getLatLong();
            Double shuttingDirection = null;
            Double focalLength = null;
            if (latLong != null) {
                shuttingDirection = exif.getAttributeDouble("GPSImgDirection", -1);
                if (shuttingDirection == -1) {
                    shuttingDirection = null;
                }
                focalLength = exif.getAttributeDouble("FocalLengthIn35mmFilm", -1);
                if (focalLength == -1) {
                    focalLength = null;
                }
            }
            return new ExifData(latLong, shuttingDirection, focalLength);
        } catch (IOException e) {
            return new ExifData(null, null, null);
        }
    }

    private Bitmap downloadGoogleMap(OkHttpClient client, double[] latLong, int zoom, boolean addMarker) {
        String url = URL_GOOGLE_STATIC_MAP +
                "?center=" + latLong[0] + "," + latLong[1] +
                "&size=" + MAP_SIZE + "x" + MAP_SIZE +
                "&zoom=" + (zoom) +
                (addMarker ? "&markers=" + latLong[0] + "," + latLong[1] : "") +
                "&key=" + Settings.getGoogleStaticMapKey();
        Request loadMapRequest = new Request.Builder().url(url).build();
        try (Response response = client.newCall(loadMapRequest).execute()) {
            if (D) Log.d(TAG, "Network response:" + response.networkResponse());
            if (response.body() != null) {
                return BitmapFactory.decodeStream(response.body().byteStream());
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private void drawGoogleMap(Bitmap baseBitmap, float angleOfView, Double shootingDirection,
                               Bitmap mapBitmap) {
        Bitmap bitmap = Bitmap.createBitmap(MAP_SIZE, MAP_SIZE,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        String shape = Settings.getMapShape(context);
        if (shape.equals(context.getString(R.string.preference_circle))) {
            // cut off the map to a circle and the copyright footer
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(0XFF000000);
            canvas.drawCircle(MAP_SIZE / 2, MAP_SIZE / 2, MAP_SIZE / 2, paint);
            int copyrightHeight = 25;
            canvas.drawRect(0, MAP_SIZE - copyrightHeight, MAP_SIZE - 1, MAP_SIZE - 1, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(mapBitmap, 0, 0, paint);
        } else {
            canvas.drawBitmap(mapBitmap, 0, 0, null);
        }
        if (Settings.shouldApplyGPSDirection(context) && shootingDirection != null) {
            // draw an arc on the map showing the photographed area
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(0X40000000);
            int padding = 10;
            int northDirection = -90;
            canvas.drawArc(new RectF(padding, padding,
                            MAP_SIZE - 1 - padding,
                            MAP_SIZE - 1 - padding),
                    northDirection + shootingDirection.floatValue() - angleOfView / 2,
                    angleOfView, true, paint);
            paint.setColor(0XFF800000);
            canvas.drawCircle(MAP_SIZE / 2, MAP_SIZE / 2, 3, paint);
        }
        // draw the map
        int left = baseBitmap.getWidth() - MAP_MARGIN - MAP_SIZE;
        int top = MAP_MARGIN;
        Paint paint = new Paint();
        paint.setAlpha(255 - (int) (2.55 * Settings.getMapTransparency(context)));
        canvas = new Canvas(baseBitmap);
        canvas.drawBitmap(bitmap, left, top, paint);
    }

    private Bitmap downloadOsmMap(OkHttpClient client, double[] latLong, int zoom) {
        // Mercator projection
        int xLargeMap = (int) Math.floor(256 * ((latLong[1] + 180) / 360 * (1 << zoom)));
        int yLargeMap = (int) Math.floor(256 * ((1 - Math.log(Math.tan(Math.toRadians(latLong[0]))
                + 1 / Math.cos(Math.toRadians(latLong[0]))) / Math.PI) / 2 * (1 << zoom)));
        //TODO out of the map
        int xLeftTopTile = (xLargeMap - MAP_SIZE / 2) / 256;
        int yLeftTopTile = (yLargeMap - MAP_SIZE / 2) / 256;
        int xRightBottomTile = (xLargeMap + MAP_SIZE / 2) / 256;
        int yRightBottomTile = (yLargeMap + MAP_SIZE / 2) / 256;
        Bitmap osmFragment = downloadOsmFragment(client, xLeftTopTile, yLeftTopTile,
                1 + xRightBottomTile - xLeftTopTile,
                1 + yRightBottomTile - yLeftTopTile, zoom);
        int x = (xLargeMap - MAP_SIZE / 2) % 256;
        int y = (yLargeMap - MAP_SIZE / 2) % 256;
        if (osmFragment != null) {
            return Bitmap.createBitmap(osmFragment, x, y, MAP_SIZE, MAP_SIZE);
        } else {
            return null;
        }
    }

    private Bitmap downloadOsmFragment(OkHttpClient client, int xTile, int yTile,
                                       int xCount, int yCount, int zoom) {
        Bitmap bitmap = Bitmap.createBitmap(xCount * MAP_SIZE, yCount * MAP_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        for (int x = 0; x < xCount; x++){
            for (int y = 0; y < yCount; y++){
                String url = "https://tile.openstreetmap.org/"
                        + zoom + "/" + (xTile + x) + "/" + (yTile + y) + ".png";
                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", USER_AGENT_FOR_OSM)
                        .build();
                try (Response response = client.newCall(request).execute()){
                    if (D) Log.d(TAG, "Network response:" + response.networkResponse());
                    if (response.body() != null) {
                        Bitmap tile = BitmapFactory.decodeStream(response.body().byteStream());
                        if (tile != null) {
                            canvas.drawBitmap(tile, x * 256, y * 256, null);
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                } catch (IOException e) {
                    return null;
                }
            }
        }
        return bitmap;
    }

    private void drawOsmMap(Bitmap baseBitmap, float angleOfView, Double shootingDirection, Bitmap mapBitmap) {
        Bitmap bitmap = Bitmap.createBitmap(MAP_SIZE, MAP_SIZE,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        String shape = Settings.getMapShape(context);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (shape.equals(context.getString(R.string.preference_circle))) {
            drawCircularOsmMap(mapBitmap, canvas);
        } else {
            drawRectangularOsmMap(mapBitmap, canvas);
        }
        if (Settings.shouldApplyGPSDirection(context) && shootingDirection != null) {
            // draw an arc on the map showing return null;
            paint.setColor(0X40000000);
            int padding = 16;
            int northDirection = -90;
            canvas.drawArc(new RectF(padding, padding, MAP_SIZE - 1 - padding, MAP_SIZE - 1 - padding),
                    northDirection + shootingDirection.floatValue() - angleOfView / 2,
                    angleOfView, true, paint);
        }
        // draw a marker
        paint.setColor(0XFF800000);
        canvas.drawCircle(MAP_SIZE / 2, MAP_SIZE / 2, 3, paint);
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(MAP_SIZE / 2, MAP_SIZE / 2, 8, paint);
        // draw the map
        int left = baseBitmap.getWidth() - MAP_MARGIN - MAP_SIZE;
        int top = MAP_MARGIN;
        paint = new Paint();
        paint.setAlpha(255 - (int) (2.55 * Settings.getMapTransparency(context)));
        canvas = new Canvas(baseBitmap);
        canvas.drawBitmap(bitmap, left, top, paint);
    }

    private void drawRectangularOsmMap(Bitmap mapBitmap, Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0XFF800000);
        canvas.drawCircle(MAP_SIZE / 2, MAP_SIZE / 2, 3, paint);
        canvas.drawBitmap(mapBitmap, 0, 0, null);
        String text = context.getString(R.string.osm_copyright);
        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);
        float textWidth = paint.measureText(text);
        canvas.drawText(text, MAP_SIZE - textWidth - 2, MAP_SIZE - 4, paint);
    }

    private void drawCircularOsmMap(Bitmap mapBitmap, Canvas canvas) {
        // cut off the map to a circle
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0XFF000000);
        canvas.drawCircle(MAP_SIZE / 2, MAP_SIZE / 2, MAP_SIZE / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(mapBitmap, 0, 0, paint);
        // draw OSM copyright on arc path
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);
        String text = context.getString(R.string.osm_copyright);
        float textWidth = paint.measureText(text);
        Path arc = new Path();
        int padding = 4;
        int bottom = 90;
        float startAngle = (float) (bottom + textWidth / 2 * 360 / (Math.PI * (MAP_SIZE - 2 * padding)));
        arc.addArc(new RectF(padding, padding,
                MAP_SIZE - 1 - padding,
                MAP_SIZE - 1 - padding), startAngle, - 360);
        canvas.drawTextOnPath(text, arc, 0, 0, paint);
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