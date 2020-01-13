package com.marteczek.photoreporter.imagehostclient.imgur;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ImgurApi {

    @GET("credits")
    Call<String> getCredits(@Header("Authorization") String clientId);

    @GET("account/me/settings")
    Call<String> getAccountSettings(@Header("Authorization") String accessToken);

    @FormUrlEncoded
    @POST("album")
    Call<String> createAlbum(@Header("Authorization") String accessToken,
            @Field("title") String title, @Field("description") String description);

    @Multipart
    @POST("upload")
    Call<String> uploadImage(@Header("Authorization") String clientId,
                             @Part() MultipartBody.Part image,
                             @Part("title") String title,
                             @Part("description") String description);

    @Multipart
    @POST("upload")
    Call<String> uploadImageToAlbum(@Header("Authorization") String accessToken,
                                    @Part() MultipartBody.Part image,
                                    @Part("title") String title,
                                    @Part("description") String description,
                                    @Part("album") String album);

}
