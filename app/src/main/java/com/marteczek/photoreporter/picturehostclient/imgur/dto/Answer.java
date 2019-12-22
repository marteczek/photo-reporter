package com.marteczek.photoreporter.picturehostclient.imgur.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Answer<T> {
    //Sample {"data":{"id":"IrBbLW7","deletehash":"mawZdBD3rz2VEDQ"},"success":true,"status":200}
    T data;
    boolean success;
    int status;
}
