package com.marteczek.photoreporter.picturehostclient.testclient;

import android.util.Log;

import com.marteczek.photoreporter.picturehostclient.BaseResponse;
import com.marteczek.photoreporter.picturehostclient.ImageHostClient;
import com.marteczek.photoreporter.picturehostclient.imgur.ImgurClient;
import com.marteczek.photoreporter.picturehostclient.imgur.data.PictureMetadata;

import static com.marteczek.photoreporter.application.Settings.Debug.E;
import static com.marteczek.photoreporter.application.Settings.Debug.TEST_UPLOAD_CLIENT_SIMULATE_ERROR;

public class TestClient implements ImageHostClient {
    private static final String TAG = "ImageHostClient";
    private BaseResponse response;

    public TestClient() {
        if (TEST_UPLOAD_CLIENT_SIMULATE_ERROR) {
            response = new ImgurClient.ImgurResponse(true, false, true, false, "",
                    PictureMetadata.builder().link("link").build(), 0L,0L,null, true);
        } else {
            response = new BaseResponse(true, false, true, false, "",
                    PictureMetadata.builder().link("").build());
        }
    }

    @Override
    public BaseResponse connect(int pictureCount) {
        return response;
    }

    @Override
    public BaseResponse createAlbum(String title, String description) {
        return response;
    }

    @Override
    public BaseResponse uploadImage(String path, String title, String description) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            if(E) Log.e(TAG, "InterruptedException", e);
        }
        return response;
    }

    @Override
    public BaseResponse disconnect() {
        return response;
    }
}
