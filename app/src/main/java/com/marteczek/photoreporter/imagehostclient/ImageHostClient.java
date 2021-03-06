package com.marteczek.photoreporter.imagehostclient;

public interface ImageHostClient {

    BaseResponse connect(int pictureCount);

    BaseResponse createAlbum(String title, String description);

    BaseResponse uploadImage(String path, String title, String description);

    BaseResponse disconnect();
}
