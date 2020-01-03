package com.marteczek.photoreporter.picturehostclient.imgur;

import android.content.Context;

import com.marteczek.photoreporter.picturehostclient.BaseResponse;
import com.marteczek.photoreporter.picturehostclient.ImageHostClient;

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
