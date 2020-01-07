package com.marteczek.photoreporter.service;

import android.database.sqlite.SQLiteException;

import com.marteczek.photoreporter.database.ReportDatabaseHelper;
import com.marteczek.photoreporter.database.dao.ItemDao;
import com.marteczek.photoreporter.database.dao.PostDao;
import com.marteczek.photoreporter.database.dao.ReportDao;
import com.marteczek.photoreporter.database.entity.Item;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.database.entity.type.ReportStatus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PostServiceTest {

    @Mock
    PostDao postDao;

    @Mock
    ItemDao itemDao;

    @Mock
    ReportDao reportDao;

    @Mock
    OnErrorListener onErrorListener;

    private ReportDatabaseHelper reportDatabaseHelper = new ReportDatabaseHelperTestImpl();

    @Test
    public void generatePosts_reportHas3Items5PicturesPerPost_oneCorrectPostIsCreated() {
        //given
        PostService postService = new PostService(postDao, itemDao, reportDao, reportDatabaseHelper);
        Report report = Report.builder().id(1L).build();
        List<Item> items = new ArrayList<>();
        items.add(Item.builder().id(1L).reportId(1L).pictureUrl("url1").header("h1").build());
        items.add(Item.builder().id(2L).reportId(1L).pictureUrl("url2").build());
        items.add(Item.builder().id(3L).reportId(1L).pictureUrl("url3").header("h3").build());
        when(reportDao.findById(1L)).thenReturn(report);
        when((itemDao.findByReportIdOrderBySuccession(1L))).thenReturn(items);
        //when
        postService.generatePosts(1L, 5, null, null);
        //then
        ArgumentCaptor<Long> idCapator = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> contentCapator = ArgumentCaptor.forClass(String.class);
        verify(postDao, times(1)).updateContentById(idCapator.capture(), contentCapator.capture());
        String content = contentCapator.getAllValues().get(0);
        String sample ="1. h1 [IMG]url1[/IMG] 2. [IMG]url2[/IMG] 3. h3 [IMG]url3[/IMG]";
        String pattern = generatePattern(sample);
        assertTrue(content.matches(pattern));
    }

    @Test
    public void generatePosts_reportHas3ItemsAllPicturesInOnePost_oneCorrectPostIsCreated() {
        //given
        PostService postService = new PostService(postDao, itemDao, reportDao, reportDatabaseHelper);
        Report report = Report.builder().id(1L).build();
        List<Item> items = new ArrayList<>();
        items.add(Item.builder().id(1L).reportId(1L).pictureUrl("url1").header("h1").build());
        items.add(Item.builder().id(2L).reportId(1L).pictureUrl("url2").build());
        items.add(Item.builder().id(3L).reportId(1L).pictureUrl("url3").header("h3").build());
        when(reportDao.findById(1L)).thenReturn(report);
        when((itemDao.findByReportIdOrderBySuccession(1L))).thenReturn(items);
        //when
        postService.generatePosts(1L, 0, null, null);
        //then
        ArgumentCaptor<Long> idCapator = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> contentCapator = ArgumentCaptor.forClass(String.class);
        verify(postDao, times(1)).updateContentById(idCapator.capture(), contentCapator.capture());
        String content = contentCapator.getAllValues().get(0);
        String sample ="1. h1 [IMG]url1[/IMG] 2. [IMG]url2[/IMG] 3. h3 [IMG]url3[/IMG]";
        String pattern = generatePattern(sample);
        assertTrue(content.matches(pattern));
    }

    @Test
    public void generatePosts_reportHas4ItemsTwoPicturesPerPost_twoCorrectPostsAreCreated() {
        //given
        PostService postService = new PostService(postDao, itemDao, reportDao, reportDatabaseHelper);
        Report report = Report.builder().id(1L).build();
        List<Item> items = new ArrayList<>();
        items.add(Item.builder().id(1L).reportId(1L).pictureUrl("url1").header("h1").build());
        items.add(Item.builder().id(2L).reportId(1L).pictureUrl("url2").build());
        items.add(Item.builder().id(3L).reportId(1L).pictureUrl("url3").header("h3").build());
        items.add(Item.builder().id(4L).reportId(1L).pictureUrl("url4").header("h4").build());
        when(reportDao.findById(1L)).thenReturn(report);
        when((itemDao.findByReportIdOrderBySuccession(1L))).thenReturn(items);
        //when
        postService.generatePosts(1L, 2, null, null);
        //then
        ArgumentCaptor<Long> idCapator = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> contentCapator = ArgumentCaptor.forClass(String.class);
        verify(postDao, times(2)).updateContentById(idCapator.capture(), contentCapator.capture());
        String content = contentCapator.getAllValues().get(0);
        String sample ="1 / 2 1. h1 [IMG]url1[/IMG] 2. [IMG]url2[/IMG]";
        String pattern = generatePattern(sample);
        assertTrue(content.matches(pattern));
        content = contentCapator.getAllValues().get(1);
        sample ="2 / 2 3. h3 [IMG]url3[/IMG] 4. h4 [IMG]url4[/IMG]";
        pattern = generatePattern(sample);
        assertTrue(content.matches(pattern));
    }

    @Test
    public void generatePosts_reportFooterIsSet_oneCorrectPostIsCreated() {
        //given
        PostService postService = new PostService(postDao, itemDao, reportDao, reportDatabaseHelper);
        Report report = Report.builder().id(1L).build();
        List<Item> items = new ArrayList<>();
        items.add(Item.builder().id(1L).reportId(1L).pictureUrl("url1").header("h1").build());
        when(reportDao.findById(1L)).thenReturn(report);
        when((itemDao.findByReportIdOrderBySuccession(1L))).thenReturn(items);
        //when
        postService.generatePosts(1L, 2, "footer", null);
        //then
        ArgumentCaptor<Long> idCapator = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> contentCapator = ArgumentCaptor.forClass(String.class);
        verify(postDao, times(1)).updateContentById(idCapator.capture(), contentCapator.capture());
        String content = contentCapator.getAllValues().get(0);
        String sample ="1. h1 [IMG]url1[/IMG] footer";
        String pattern = generatePattern(sample);
        assertTrue(content.matches(pattern));
    }

    @Test
    public void generatePosts_reportHasItems_statusOfReportIsPostCreated() {
        //given
        PostService postService = new PostService(postDao, itemDao, reportDao, reportDatabaseHelper);
        Report report = Report.builder().id(1L).build();
        List<Item> items = new ArrayList<>();
        items.add(Item.builder().id(1L).reportId(1L).pictureUrl("url1").header("h1").build());
        when((itemDao.findByReportIdOrderBySuccession(1L))).thenReturn(items);
        when(reportDao.findById(1L)).thenReturn(report);
        //when
        postService.generatePosts(1L, 2, null, null);
        //then
        verify(reportDao, times(1)).updateStatusById(1L, ReportStatus.POST_CREATED);
    }

    @Test
    public void generatePosts_exceptionIsThrown_callbackFunctionIsCalled() {
        //given
        PostService postService = new PostService(postDao, itemDao, reportDao, reportDatabaseHelper);
        Report report = Report.builder().id(1L).build();
        when(reportDao.findById(1L)).thenReturn(report);
        when((itemDao.findByReportIdOrderBySuccession(1L))).thenThrow(new SQLiteException());
        //when
        postService.generatePosts(1L, 2, null, onErrorListener);
        //then
        verify(onErrorListener, times(1)).onError(any());
    }

    private String generatePattern(String source) {
        return "^\\s*" + source
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace(" ", "\\s+")
                + "\\s*$";
    }
}