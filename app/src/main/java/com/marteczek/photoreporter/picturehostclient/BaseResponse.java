package com.marteczek.photoreporter.picturehostclient;

import com.marteczek.photoreporter.picturehostclient.imgur.data.PictureMetadata;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BaseResponse {
    protected boolean success;
    protected boolean canRetry;
    protected boolean canContinue;
    protected boolean ioException;
    protected String albumMetadata;
    protected PictureMetadata pictureMetadata;
}
