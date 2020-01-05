package com.marteczek.photoreporter.picturehostclient.imgur.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;

import static org.junit.Assert.*;

public class AnswerTest {

    @Test
    public void answerWithAlbum_exampleResponseBody_jsonConvertedToObject() {
        //given
        String exampleBody = "{\"data\":{\"id\":\"IrBbLW7\",\"deletehash\":\"mawZdBD3rz2VEDQ\"},\"success\":true,\"status\":200}";
        //when
        Gson gson = new GsonBuilder().create();
        Type albumType = new TypeToken<Answer<Album>>() {}.getType();
        Answer<Album> answer = gson.fromJson(exampleBody, albumType);
        //then
        assertEquals("IrBbLW7", answer.getData().getId());

    }

    @Test
    public void answerWithPicture_exampleResponseBody_jsonConvertedToObject() {
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