package com.marteczek.photoreporter.ui;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marteczek.photoreporter.application.Settings;
import com.marteczek.photoreporter.picturemanager.PictureManager;

public class PicturePreviewViewModel extends AndroidViewModel {

    private MutableLiveData<Bitmap> pictureLiveData;

    private LoadPictureTask loadPictureTask;

    private PictureManager pictureManager;

    private static class LoadPictureTask extends AsyncTask<String, Void, Void> {

        private PictureManager pictureManager;

        private int rotation;

        private int greaterDimension;

        MutableLiveData<Bitmap> pictureLiveData;

        LoadPictureTask(Context context, PictureManager pictureManager, int rotation,
                               MutableLiveData<Bitmap> pictureLiveData) {
            this.pictureManager = pictureManager;
            this.rotation = rotation;
            this.pictureLiveData = pictureLiveData;
            greaterDimension = Settings.getPictureDimension(context);
        }

        @Override
        protected Void doInBackground(String... paths) {
            pictureManager.preparePictureForUpload(paths[0], rotation, greaterDimension,
                    pictureLiveData);
            return null;
        }
    }

    public PicturePreviewViewModel(@NonNull Application application, PictureManager pictureManager) {
        super(application);
        this.pictureManager = pictureManager;
    }

    @Override
    protected void onCleared() {
        if (loadPictureTask != null) {
            loadPictureTask.cancel(true);
        }
        super.onCleared();
    }

    LiveData<Bitmap> getPicture(String path, int rotation) {
        if (pictureLiveData == null) {
            pictureLiveData = new MutableLiveData<>();
            loadPictureTask = new LoadPictureTask(getApplication(), pictureManager, rotation,
                    pictureLiveData);
            loadPictureTask.execute(path);
        }
        return pictureLiveData;
    }
}