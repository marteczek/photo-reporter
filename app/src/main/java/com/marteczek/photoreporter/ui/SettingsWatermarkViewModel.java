package com.marteczek.photoreporter.ui;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marteczek.photoreporter.application.Settings;
import com.marteczek.photoreporter.picturemanager.PictureManager;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import static com.marteczek.photoreporter.application.Settings.Debug.E;

public class SettingsWatermarkViewModel extends AndroidViewModel {

    private static final String TAG = "SettingsWatermarkVM";

    private static final String WATERMARK_THUMBNAIL_FILE_NAME = "watermark_thumbnail";

    private static final int THUMBNAIL_SIZE = 100;

    private PictureManager pictureManager;

    private SetWatermarkTask setWatermarkTask;

    private LoadWatermarkThumbnailTask loadWatermarkThumbnailTask;

    private MutableLiveData<Bitmap> watermarkThumbnail;

    private final String watermarkThumbnailPath;

    private static class SetWatermarkTask extends AsyncTask<Uri, Void, Void> {

        private PictureManager pictureManager;

        private MutableLiveData<Bitmap> watermarkThumbnail;

        private SetWatermarkTask(PictureManager pictureManager,
                                MutableLiveData<Bitmap> watermarkThumbnail) {
            this.pictureManager = pictureManager;
            this.watermarkThumbnail = watermarkThumbnail;
        }

        @Override
        protected Void doInBackground(Uri... uris) {
            Uri watermarkUri = uris[0];
            String watermarkFileName = Settings.WATERMARK_FILE_NAME;
            try {
                String watermarkPath = pictureManager.copy(watermarkUri, watermarkFileName);
                String thumbnailPath = pictureManager.generateThumbnail(watermarkPath,
                        WATERMARK_THUMBNAIL_FILE_NAME, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
                Bitmap bitmap = BitmapFactory.decodeFile(thumbnailPath);
                watermarkThumbnail.postValue(bitmap);
            } catch (IOException e) {
                if(E) Log.e(TAG, "IOException", e);
            }
            return null;
        }
    }

    private static class LoadWatermarkThumbnailTask extends AsyncTask<String, Void, Void> {

        private MutableLiveData<Bitmap> watermarkThumbnail;

        private LoadWatermarkThumbnailTask(MutableLiveData<Bitmap> watermarkThumbnail) {
            this.watermarkThumbnail = watermarkThumbnail;
        }

        @Override
        protected Void doInBackground(String... paths) {
            Bitmap bitmap = BitmapFactory.decodeFile(paths[0]);
            watermarkThumbnail.postValue(bitmap);
            return  null;
        }
    }

    @Inject
    public SettingsWatermarkViewModel(@NonNull Application application, PictureManager pictureManager) {
        super(application);
        this.pictureManager = pictureManager;
        watermarkThumbnailPath = new File(pictureManager.getFilesDir(), WATERMARK_THUMBNAIL_FILE_NAME)
                .getAbsolutePath();
    }

    LiveData<Bitmap> getWatermarkThumbnail() {
        if (watermarkThumbnail == null) {
            watermarkThumbnail = new MutableLiveData<>();
            loadWatermarkThumbnailTask = new LoadWatermarkThumbnailTask(watermarkThumbnail);
            loadWatermarkThumbnailTask.execute(watermarkThumbnailPath);
        }
        return watermarkThumbnail;
    }

    @Override
    protected void onCleared() {
        if (setWatermarkTask != null) {
            setWatermarkTask.cancel(true);
        }
        if (loadWatermarkThumbnailTask != null) {
            loadWatermarkThumbnailTask.cancel(true);
        }
        super.onCleared();
    }

    void setWatermark(Uri watermarkUri) {
        if (setWatermarkTask != null) {
            setWatermarkTask.cancel(true);
        }
        setWatermarkTask = new SetWatermarkTask(pictureManager, watermarkThumbnail);
        setWatermarkTask.execute(watermarkUri);
    }
}
