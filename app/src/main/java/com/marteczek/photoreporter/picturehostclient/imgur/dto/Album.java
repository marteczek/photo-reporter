package com.marteczek.photoreporter.picturehostclient.imgur.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Album {
    //Sample {"data":{"id":"IrBbLW7","deletehash":"mawZdBD3rz2VEDQ"},"success":true,"status":200}
    String id;
    String deletehash;
}
