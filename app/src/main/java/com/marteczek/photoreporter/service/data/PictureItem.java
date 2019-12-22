package com.marteczek.photoreporter.service.data;


import android.net.Uri;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class PictureItem {
    private String header;
    private String picturePath;
    private Uri pictureUri;
    private Date lastModified;
}
