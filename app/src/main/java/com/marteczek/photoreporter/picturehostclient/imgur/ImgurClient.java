package com.marteczek.photoreporter.picturehostclient.imgur;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.marteczek.photoreporter.application.Settings;
import com.marteczek.photoreporter.picturehostclient.ImageHostClient;
import com.marteczek.photoreporter.picturehostclient.BaseResponse;
import com.marteczek.photoreporter.picturehostclient.imgur.dto.Album;
import com.marteczek.photoreporter.picturehostclient.imgur.dto.Answer;
import com.marteczek.photoreporter.picturehostclient.imgur.dto.Picture;
import com.marteczek.photoreporter.picturehostclient.imgur.data.PictureMetadata;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;

import lombok.Builder;
import lombok.Getter;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import static com.marteczek.photoreporter.application.Settings.Debug.D;
import static com.marteczek.photoreporter.application.Settings.Debug.E;

public class ImgurClient implements ImageHostClient {

    private static final String TAG = "ImgurClient";

    private static final String BASE_URL = "https://api.imgur.com/3/";

    private static final int RESERVED_CREDITS = 500;

    private final ImgurApi imgurAPI;

    private final Gson gson;

    private final String restClientId;

    private final String restAccessToken;

    private String albumId;

    @Getter
    public static class ImgurResponse extends BaseResponse {
        private Long rateLimitClientRemaining;
        private Long rateLimitUserRemaining;
        private Date rateLimitUserReset;
        private boolean enoughCredits;

        @Builder
        public ImgurResponse(boolean success, boolean canRetry, boolean canContinue,
                             boolean ioException, String albumMetadata, PictureMetadata pictureMetadata,
                             Long rateLimitClientRemaining, Long rateLimitUserRemaining,
                             Date rateLimitUserReset, boolean enoughCredits) {
            super(success, canRetry, canContinue, ioException, albumMetadata, pictureMetadata);
            this.rateLimitClientRemaining = rateLimitClientRemaining;
            this.rateLimitUserRemaining = rateLimitUserRemaining;
            this.rateLimitUserReset = rateLimitUserReset;
            this.enoughCredits = enoughCredits;
        }
    }

    public ImgurClient(Context context) {
        String clientId = Settings.getClientId(context);
        String accessToken = Settings.getImgurUserData(context).getAccessToken();
        restClientId = "Client-ID " + clientId;
        restAccessToken = "Bearer " + accessToken;
        imgurAPI = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(ImgurApi.class);
        gson = new GsonBuilder().create();
    }

    @Override
    public BaseResponse connect(int pictureCount)  {
        Call<String> call = imgurAPI.getAccountSettings(restAccessToken);
        Response<String> response;
        try {
            response = call.execute();
        } catch(IOException e) {
            return ImgurResponse.builder().success(false).ioException(true).canRetry(true).build();
        }
        if (response.isSuccessful()) {
            Long rateLimitClientRemaining = null;
            Long rateLimitUserRemaining = null;
            Date rateLimitUserReset = null;
            try {
                rateLimitClientRemaining = Long.valueOf(
                        response.headers().get("x-ratelimit-clientremaining"));
            } catch (NumberFormatException e) {
                if(E) Log.e(TAG, "NumberFormatException", e);
            }
            try {
                rateLimitUserRemaining = Long.valueOf(
                        response.headers().get("x-ratelimit-userremaining"));
            } catch (NumberFormatException e) {
                if(E) Log.e(TAG, "NumberFormatException", e);
            }
            try {
                Long userReset = Long.valueOf(response.headers().get("x-ratelimit-userreset"));
                rateLimitUserReset = new Date(userReset * 1000);
            } catch (NumberFormatException e) {
                if(E) Log.e(TAG, "NumberFormatException", e);
            }
            if(D) Log.d(TAG, "x-ratelimit-clientremaining: " + rateLimitClientRemaining);
            if(D) Log.d(TAG, "rateLimitUserRemaining: " + rateLimitUserRemaining);
            if(D) Log.d(TAG, "rateLimitUserReset: " + rateLimitUserReset);
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
        File file = new File(path);
        RequestBody fileReqBody = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("image", file.getName(), fileReqBody);
        Call<String> call = imgurAPI.uploadImage(restAccessToken, part,  title, description, albumId);
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

    @Override
    public BaseResponse disconnect() {
        return ImgurResponse.builder()
                .success(true)
                .canContinue(true)
                .build();
    }
}
