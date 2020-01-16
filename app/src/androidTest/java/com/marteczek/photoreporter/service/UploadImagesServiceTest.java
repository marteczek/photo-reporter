package com.marteczek.photoreporter.service;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.marteczek.photoreporter.database.ReportDatabase;
import com.marteczek.photoreporter.database.dao.ItemDao;
import com.marteczek.photoreporter.database.dao.ReportDao;
import com.marteczek.photoreporter.database.entity.Item;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.database.entity.type.ElementStatus;
import com.marteczek.photoreporter.database.entity.type.ReportStatus;
import com.marteczek.photoreporter.imagehostclient.BaseResponse;
import com.marteczek.photoreporter.imagehostclient.ImageHostClient;
import com.marteczek.photoreporter.imagehostclient.imgur.data.PictureMetadata;
import com.marteczek.photoreporter.picturemanager.PictureManager;
import com.marteczek.photoreporter.service.misc.PictureFormat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(AndroidJUnit4.class)
public class UploadImagesServiceTest {

    @Mock
    PictureManager pictureManager;

    @Mock
    ImageHostClient client;

    private UploadImagesService.UploadManager uploadManager = () -> false;

    private ReportDao reportDao;

    private ItemDao itemDao;

    private ReportDatabase db;

    private UploadImagesService service;

    private PictureFormat pictureFormat = PictureFormat.builder().greaterDimension(1024).build();

    @Before
    public void configure() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, ReportDatabase.class).build();
        reportDao = db.reportDao();
        itemDao = db.itemDao();
        MockitoAnnotations.initMocks(this);
        service = new UploadImagesService(pictureManager, reportDao, itemDao);
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void uploadImages_reportWithOneItemProvided_onePictureIsUploaded() {
        //given
        configureClientWithResponseSuccess();
        long reportId = insertReportWithOneItem();
        //when
        service.uploadImages(reportId, client, pictureFormat, uploadManager, null);
        //then
        verify(client, times(1)).connect(anyInt());
        verify(client, times(1)).createAlbum(any(), any());
        verify(client, times(1)).uploadImage(any(), any(), any());
        verify(client, times(1)).disconnect();
        Report report = reportDao.findById(reportId);
        assertNotNull(report);
        List<Item> items = itemDao.findByReportId(reportId);
        assertEquals(1, items.size());
    }

    @Test
    public void uploadImages_reportWithOneItemProvided_correctStatusesAreSet() {
        //given
        configureClientWithResponseSuccess();
        long reportId = insertReportWithOneItem();
        //when
        service.uploadImages(reportId, client, pictureFormat, uploadManager, null);
        //then
        Report report = reportDao.findById(reportId);
        assertEquals(ReportStatus.SENT, report.getStatus());
        List<Item> items = itemDao.findByReportId(reportId);
        assertEquals(1, items.size());
        assertEquals(ElementStatus.SENT, items.get(0).getStatus());
        assertEquals("link", items.get(0).getPictureUrl());
    }

    @Test
    public void uploadImages_oneErrorDuringPictureUploading_nextAttemptIsTaken() {
        //given
        BaseResponse responseSuccess = new BaseResponse(true, false, true, false,
                "albumMetadata",
                new PictureMetadata("link", "pictureMetadata"));
        BaseResponse responseRetry = new BaseResponse(false, true, false, false,
                "albumMetadata",
                new PictureMetadata("link", "pictureMetadata"));
        when(client.connect(anyInt())).thenReturn(responseSuccess);
        when(client.createAlbum(any(), any())).thenReturn(responseSuccess);
        when(client.uploadImage(any(), any(), any())).thenReturn(responseRetry, responseSuccess);
        when(client.disconnect()).thenReturn(responseSuccess);
        long reportId = insertReportWithOneItem();
        //when
        service.uploadImages(reportId, client, pictureFormat, uploadManager, null);
        //then
        verify(client, times(2)).uploadImage(any(), any(), any());
    }

    private long insertReportWithOneItem() {
        Date date = new Date(0L);
        Report report = Report.builder().name("report").status(ReportStatus.NEW).date(date).build();
        long reportId = reportDao.insert(report);
        Item item = Item.builder().reportId(reportId).status(ElementStatus.NEW)
                .header("header").picturePath("path").build();
        itemDao.insert(item);
        return reportId;
    }

    private void configureClientWithResponseSuccess() {
        BaseResponse responseSuccess = new BaseResponse(true, false, true, false,
                "albumMetadata",
                new PictureMetadata("link", "pictureMetadata"));
        assertNotNull(client);
        when(client.connect(anyInt())).thenReturn(responseSuccess);
        when(client.createAlbum(any(), any())).thenReturn(responseSuccess);
        when(client.uploadImage(any(), any(), any())).thenReturn(responseSuccess);
        when(client.disconnect()).thenReturn(responseSuccess);
    }
}