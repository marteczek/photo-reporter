package com.marteczek.photoreporter.service;

import android.content.Context;
import android.net.Uri;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.marteczek.photoreporter.application.MainThreadRunner;
import com.marteczek.photoreporter.database.ReportDatabase;
import com.marteczek.photoreporter.database.ReportDatabaseHelper;
import com.marteczek.photoreporter.database.dao.ItemDao;
import com.marteczek.photoreporter.database.dao.PostDao;
import com.marteczek.photoreporter.database.dao.ReportDao;
import com.marteczek.photoreporter.database.entity.Item;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.database.entity.type.ReportStatus;
import com.marteczek.photoreporter.picturemanager.PictureManager;
import com.marteczek.photoreporter.service.data.PictureItem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class ReportServiceTest {

    private ReportDao reportDao;

    private ItemDao itemDao;

    private PostDao postDao;

    @Mock
    PictureManager pictureManager;

    private ReportDatabaseHelper reportDatabaseHelper = new ReportDatabaseHelperTestImpl();

    private MainThreadRunner mainThreadRunner = new MainThreadRunnerTestImpl();

    private ReportDatabase db;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, ReportDatabase.class).build();
        reportDao = db.reportDao();
        itemDao = db.itemDao();
        postDao = db.postDao();
   }

   @Before
   public void intMocks() {
       MockitoAnnotations.initMocks(this);
   }

    @After
    public void closeDb(){
        db.close();
    }

    @Test
    public void insertItemsToReport_onePhotoProvided_correctItemIsInserted() {
        //given
        try {
            when(pictureManager.copy(any(), any())).thenReturn("path");
        } catch (IOException e) {
            //ignore
        }
        ReportService service = new ReportService(reportDao, itemDao, postDao, pictureManager,
                reportDatabaseHelper, mainThreadRunner);
        Report report = Report.builder().id(1L).name("R1").status(ReportStatus.NEW).build();
        Long reportId  = reportDao.insert(report);
        List<PictureItem> pictures = new ArrayList<>();
        pictures.add(PictureItem.builder().pictureUri(Uri.parse("test:1")).build());
        //when
        service.insertItemsToReport(reportId, pictures, 200, null, null);
        //then
        List<Item> items = itemDao.findAll();
        assertEquals(1, items.size());
        Item item = items.get(0);
        assertNotNull(item.getPicturePath());
        assertNotNull(item.getPictureUri());
        assertNotNull(item.getSuccession());
    }

    @Test
    public void insertItemsToReport_threePhotosProvided_itemsAreInsertedInTheRightOrder() {
        //given
        ReportService service = new ReportService(reportDao, itemDao, postDao, pictureManager,
                reportDatabaseHelper, mainThreadRunner);
        Report report = Report.builder().id(1L).name("R1").status(ReportStatus.NEW).build();
        Long reportId  = reportDao.insert(report);
        List<PictureItem> pictures = new ArrayList<>();
        Uri[] uris = new Uri[]{Uri.parse("test:1"), Uri.parse("test:2"), Uri.parse("test:3")};
        pictures.add(PictureItem.builder().pictureUri(uris[0]).build());
        pictures.add(PictureItem.builder().pictureUri(uris[1]).build());
        pictures.add(PictureItem.builder().pictureUri(uris[2]).build());
        //when
        service.insertItemsToReport(reportId, pictures, 200, null, null);
        //then
        List<Item> items = itemDao.findAll();
        assertEquals(3, items.size());
        assertEquals("test:1", items.get(0).getPictureUri());
        assertEquals("test:2", items.get(1).getPictureUri());
        assertEquals("test:3", items.get(2).getPictureUri());
    }
}