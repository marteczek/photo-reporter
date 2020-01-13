package com.marteczek.photoreporter.imagehostclient;

import com.marteczek.photoreporter.imagehostclient.imgur.data.PictureMetadata;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BaseResponse {
    protected boolean success;
    protected boolean retryable;
    protected boolean continuable;
    protected boolean ioException;
    protected String albumMetadata;
    protected PictureMetadata pictureMetadata;
}
