package com.marteczek.photoreporter.picturehostclient.imgur.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class PictureMetadata {
    String link;
    String metadata;
}
