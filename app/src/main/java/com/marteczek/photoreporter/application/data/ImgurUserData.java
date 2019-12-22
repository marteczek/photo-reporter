package com.marteczek.photoreporter.application.data;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ImgurUserData {
    String accessToken;
    Date expiresOn;
    String refreshToken;
    String accountUsername;
    String accountId;
}
