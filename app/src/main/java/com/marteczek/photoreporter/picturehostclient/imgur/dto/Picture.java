package com.marteczek.photoreporter.picturehostclient.imgur.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Picture {
    String id;
    String title;
    String description;
    Long datetime;
    String type;
    Boolean animated;
    Integer width;
    Integer height;
    Integer size;
    String deletehash;
    String link;
}
