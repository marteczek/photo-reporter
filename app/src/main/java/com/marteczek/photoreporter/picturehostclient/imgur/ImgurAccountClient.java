package com.marteczek.photoreporter.picturehostclient.imgur;

import android.content.Context;

import com.google.gson.reflect.TypeToken;
import com.marteczek.photoreporter.picturehostclient.ImageHostClient;
import com.marteczek.photoreporter.picturehostclient.BaseResponse;
import com.marteczek.photoreporter.picturehostclient.imgur.dto.Album;
import com.marteczek.photoreporter.picturehostclient.imgur.dto.Answer;

import java.io.IOException;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.Response;

public class ImgurAccountClient extends ImgurAbstractClient implements ImageHostClient {

    private static final String TAG = "ImgurAccountClient";

    private String albumId;

    public ImgurAccountClient(Context context) {
        super(context);
    }

    @Override
    public BaseResponse connect(int pictureCount)  {
        BaseResponse baseResponse = checkCredits(pictureCount);
        Call<String> call = imgurAPI.getAccountSettings(restAccessToken);
        Response<String> response;
        try {
            response = call.execute();
        } catch(IOException e) {
            return ImgurResponse.builder().success(false).ioException(true).canRetry(true).build();
        }
        if (response.isSuccessful()) {
            return baseResponse;
        } else {
            return ImgurResponse.builder()
                    .success(false)
                    .canRetry(false)
                    .canContinue(false)
                    .loggedIn(false)
                    .build();
        }
    }

    @Override
    public BaseResponse createAlbum(String title, String description) {
        Call<String> call = imgurAPI.createAlbum(restAccessToken, title, description);
        Response<String> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            return ImgurResponse.builder()
                    .success(false)
                    .canContinue(false)
                    .canRetry(true)
                    .build();
        }
        if (response.isSuccessful()) {
            Type albumType = new TypeToken<Answer<Album>>() {}.getType();
            Answer<Album> album = gson.fromJson(response.body(), albumType);
            albumId = album.getData().getId();
            return ImgurResponse.builder()
                    .success(true)
                    .canContinue(true)
                    .albumMetadata(gson.toJson(album.getData()))
                    .build();
        }
        return ImgurResponse.builder()
                .success(false)
                .canRetry(false)
                .canContinue(false)
                .build();
    }

    @Override
    public BaseResponse uploadImage(String path, String title, String description){
        return super.uploadImage(path, title, description, albumId);
    }

    @Override
    public BaseResponse disconnect() {
        return ImgurResponse.builder()
                .success(true)
                .canContinue(true)
                .build();
    }
}
