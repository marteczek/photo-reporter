package com.marteczek.photoreporter.picturehostclient.imgur;

import com.marteczek.photoreporter.picturehostclient.BaseResponse;
import com.marteczek.photoreporter.picturehostclient.imgur.data.PictureMetadata;

import java.util.Date;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ImgurResponse extends BaseResponse {
    private Long rateLimitClientRemaining;
    private Long rateLimitUserRemaining;
    private Date rateLimitUserReset;
    private Boolean enoughCredits;
    private Boolean loggedIn;

    @Builder
    public ImgurResponse(boolean success, boolean canRetry, boolean canContinue,
                         boolean ioException, String albumMetadata, PictureMetadata pictureMetadata,
                         Long rateLimitClientRemaining, Long rateLimitUserRemaining,
                         Date rateLimitUserReset, Boolean enoughCredits, Boolean loggedIn) {
        super(success, canRetry, canContinue, ioException, albumMetadata, pictureMetadata);
        this.rateLimitClientRemaining = rateLimitClientRemaining;
        this.rateLimitUserRemaining = rateLimitUserRemaining;
        this.rateLimitUserReset = rateLimitUserReset;
        this.enoughCredits = enoughCredits;
        this.loggedIn = loggedIn;
    }
}

