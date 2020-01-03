package com.marteczek.photoreporter.picturehostclient.imgur;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.marteczek.photoreporter.picturehostclient.imgur.dto.Answer;
import com.marteczek.photoreporter.picturehostclient.imgur.dto.Credits;

import org.junit.Test;

import java.lang.reflect.Type;

import static org.junit.Assert.*;

public class ImgurAnonymousClientTest {

    @Test
    public void connect_exampleResponseBody_jsonConvertedToObject() {
        //given
        String exampleBody = "{\"data\":{\"UserLimit\":500,\"UserRemaining\":500,\"UserReset\":1578066561,\"ClientLimit\":12500,\"ClientRemaining\":12497},\"success\":true,\"status\":200}";
        //when
        Gson gson = new GsonBuilder().create();
        Type pictureType = new TypeToken<Answer<Credits>>() {}.getType();
        Answer<Credits> answer = gson.fromJson(exampleBody, pictureType);
        //then
        assertEquals(500L, (long) answer.getData().getUserLimit());
    }

}