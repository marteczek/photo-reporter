package com.marteczek.photoreporter.imagehostclient.imgur;

import android.content.Context;

import com.marteczek.photoreporter.imagehostclient.BaseResponse;
import com.marteczek.photoreporter.imagehostclient.ImageHostClient;

public class ImgurAnonymousClient extends ImgurAbstractClient implements ImageHostClient {

    private static final String TAG = "ImgurAnonymousClient";

    public ImgurAnonymousClient(Context context) {
        super(context);
    }

    @Override
    public BaseResponse connect(int pictureCount)  {
        return checkCredits(pictureCount);
    }

    @Override
    public BaseResponse createAlbum(String title, String description) {
        return ImgurResponse.builder()
                .success(true)
                .canContinue(true)
                .build();
    }

    @Override
    public BaseResponse uploadImage(String path, String title, String description){
        return super.uploadImage(path, title, description, null);
    }

    @Override
    public BaseResponse disconnect() {
        return ImgurResponse.builder()
                .success(true)
                .canContinue(true)
                .build();
    }
}
