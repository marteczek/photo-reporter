package com.marteczek.photoreporter.picturehostclient.imgur;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ImgurApi {

    @GET("account/{userId}")
    Call<String> getAccountBase(@Header("Authorization") String clientId,
                                @Path("userId") String userId);

    @GET("account/me/settings")
    Call<String> getAccountSettings(@Header("Authorization") String accessToken);

    @FormUrlEncoded
    @POST("album")
    Call<String> createAlbum(@Header("Authorization") String accessToken,
            @Field("title") String title, @Field("description") String description);

    @Multipart
    @POST("upload")
    Call<String> uploadImage(@Header("Authorization") String accessToken,
                             @Part() MultipartBody.Part image,
                             @Part("title") String title,
                             @Part("description") String description,
                             @Part("album") String album);

}
