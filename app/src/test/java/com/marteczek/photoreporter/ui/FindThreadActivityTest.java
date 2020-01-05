package com.marteczek.photoreporter.ui;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class FindThreadActivityTest {

    @Test
    public void topicsPattern_refWithTopics_groupFound() {
        //given
        String ref ="/topics/534013?page=12";
        //when
        Matcher matcher = Pattern.compile(FindThreadActivity.TOPICS_REF_PATTERN).matcher(ref);
        String group = null;
        if (matcher.matches()) {
            group = matcher.group(1);
        }
        //then
        assertEquals("534013", group);
    }

    @Test
    public void topicsPattern_queryWithShowThread_groupFound() {
        //given
        String query ="x=1&t=1195867&page=6";
        //when
        Matcher matcher = Pattern.compile(FindThreadActivity.TOPICS_QUERY_PATTERN).matcher(query);
        String group = null;
        if (matcher.find()) {
            group = matcher.group(1);
        }
        //then
        assertEquals("1195867", group);
    }
}