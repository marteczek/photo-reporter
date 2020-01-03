package com.marteczek.photoreporter.picturehostclient.imgur.dto;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Credits {
    @SerializedName("UserLimit")
    Long userLimit;
    @SerializedName("UserRemaining")
    Long userRemaining;
    @SerializedName("UserReset")
    Long userReset;
    @SerializedName("ClientLimit")
    Long clientLimit;
    @SerializedName("ClientRemaining")
    Long clientRemaining;
}
