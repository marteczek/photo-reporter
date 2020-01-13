package com.marteczek.photoreporter.imagehostclient.imgur;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.marteczek.photoreporter.application.Settings;
import com.marteczek.photoreporter.application.data.ImgurUserData;
import com.marteczek.photoreporter.imagehostclient.BaseResponse;
import com.marteczek.photoreporter.imagehostclient.imgur.data.PictureMetadata;
import com.marteczek.photoreporter.imagehostclient.imgur.dto.Answer;
import com.marteczek.photoreporter.imagehostclient.imgur.dto.Credits;
import com.marteczek.photoreporter.imagehostclient.imgur.dto.Picture;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import static com.marteczek.photoreporter.application.Settings.Debug.D;
import static com.marteczek.photoreporter.application.Settings.Debug.E;

abstract class ImgurAbstractClient {

    private static final String TAG = "ImgurAbstractClient";

    private static final String BASE_URL = "https://api.imgur.com/3/";

    private static final int RESERVED_CREDITS = 500;

    final ImgurApi imgurAPI;

    final Gson gson;

    final String restClientId;

    final String restAccessToken;

    ImgurAbstractClient(Context context) {
        String clientId = Settings.getClientId(context);
        restClientId = "Client-ID " + clientId;
        ImgurUserData imgurUserData = Settings.getImgurUserData(context);
        if (imgurUserData != null) {
            restAccessToken = "Bearer " + imgurUserData.getAccessToken();
        } else {
            restAccessToken = null;
        }
        imgurAPI = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(ImgurApi.class);
        gson = new GsonBuilder().create();
    }

    BaseResponse checkCredits(int pictureCount) {
        Call<String> call = imgurAPI.getCredits(restClientId);
        Response<String> response;
        try {
            response = call.execute();
        } catch(IOException e) {
            return ImgurResponse.builder().success(false).ioException(true).canRetry(true).build();
        }
        if (response.isSuccessful()) {
            Type creditsType = new TypeToken<Answer<Credits>>() {}.getType();
            Credits credits;
            Answer<Credits> answer = gson.fromJson(response.body(), creditsType);
            if(answer == null || answer.getData() == null){
                if(E) Log.e(TAG, "fromJson returned null data");
                return ImgurResponse.builder()
                        .success(false)
                        .canRetry(false)
                        .canContinue(false)
                        .enoughCredits(false)
                        .build();
            }
            credits = answer.getData();
            Long rateLimitClientRemaining = credits.getClientRemaining();
            Long rateLimitUserRemaining = credits.getUserRemaining();
            Date rateLimitUserReset = null;
            try {
                if(credits.getUserReset() != null) {
                    rateLimitUserReset = new Date(credits.getUserReset() * 1000);
                }
            } catch (NumberFormatException e) {
                if(E) Log.e(TAG, "NumberFormatException", e);
            }
            if(D) Log.d(TAG, "rate limit: clientremaining: " + rateLimitClientRemaining);
            if(D) Log.d(TAG, "rate limit: UserRemaining: " + rateLimitUserRemaining);
            if(D) Log.d(TAG, "rate limit: UserReset: " + rateLimitUserReset);
            boolean enoughCredits = rateLimitClientRemaining != null
                    && rateLimitClientRemaining > (pictureCount * 10) + RESERVED_CREDITS;
            boolean success = response.isSuccessful()
                    && rateLimitClientRemaining != null && rateLimitClientRemaining > (pictureCount * 10) + RESERVED_CREDITS;
            return ImgurResponse.builder()
                    .success(success)
                    .canContinue(true)
                    .rateLimitClientRemaining(rateLimitClientRemaining)
                    .rateLimitUserRemaining(rateLimitUserRemaining)
                    .rateLimitUserReset(rateLimitUserReset)
                    .enoughCredits(enoughCredits)
                    .build();
        }
        return ImgurResponse.builder()
                .success(false)
                .canRetry(false)
                .canContinue(false)
                .build();
    }

    BaseResponse uploadImage(String path, String title, String description, String albumId){
        File file = new File(path);
        RequestBody fileReqBody = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("image", file.getName(), fileReqBody);
        Call<String> call;
        if (albumId == null) {
            call = imgurAPI.uploadImage(restClientId, part,  title, description);
        } else {
            call = imgurAPI.uploadImageToAlbum(restAccessToken, part,  title, description, albumId);
        }
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
            Long rateLimitClientRemaining = null;
            Long rateLimitUserRemaining = null;
            Date rateLimitUserReset = null;
            try {
                rateLimitClientRemaining = Long.valueOf(
                        response.headers().get("x-ratelimit-clientremaining"));
            } catch(NumberFormatException e) {
                if(E) Log.e(TAG, "NumberFormatException", e);
            }
            try {
                rateLimitUserRemaining = Long.valueOf(
                        response.headers().get("x-ratelimit-userremaining"));
            } catch(NumberFormatException e) {
                if(E) Log.e(TAG, "NumberFormatException", e);
            }
            try {
                Long userReset = Long.valueOf(response.headers().get("x-ratelimit-userreset"));
                rateLimitUserReset = new Date(userReset * 1000);
            } catch(NumberFormatException e) {
                if(E) Log.e(TAG, "NumberFormatException", e);
            }
            if(D) Log.d(TAG, "x-ratelimit-clientremaining: " + rateLimitClientRemaining);
            if(D) Log.d(TAG, "rateLimitUserRemaining: " + rateLimitUserRemaining);
            if(D) Log.d(TAG, "rateLimitUserReset: " + rateLimitUserReset);
            Type pictureType = new TypeToken<Answer<Picture>>() {}.getType();
            Answer<Picture> picture = gson.fromJson(response.body(), pictureType);
            return ImgurResponse.builder()
                    .success(true)
                    .canContinue(true)
                    .pictureMetadata(new PictureMetadata(picture.getData().getLink(),
                            gson.toJson(picture.getData())))
                    .build();
        }
        return ImgurResponse.builder()
                .success(false)
                .canContinue(false)
                .canRetry(false)
                .build();
    }
}
