package com.marteczek.photoreporter.imagehostclient.imgur.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Answer<T> {
    T data;
    boolean success;
    int status;
}
