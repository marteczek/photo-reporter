package com.marteczek.photoreporter.service;

import android.database.sqlite.SQLiteException;

import com.marteczek.photoreporter.application.MainThreadRunner;
import com.marteczek.photoreporter.database.ReportDatabaseHelper;
import com.marteczek.photoreporter.database.dao.ItemDao;
import com.marteczek.photoreporter.database.dao.PostDao;
import com.marteczek.photoreporter.database.dao.ReportDao;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.database.entity.type.ReportStatus;
import com.marteczek.photoreporter.picturemanager.PictureManager;
import com.marteczek.photoreporter.service.data.PictureItem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static com.marteczek.photoreporter.database.entity.type.ReportStatus.PENDING;
import static com.marteczek.photoreporter.database.entity.type.ReportStatus.PICTURE_SENDING_CANCELLED;
import static com.marteczek.photoreporter.database.entity.type.ReportStatus.PICTURE_SENDING_FAILURE;
import static com.marteczek.photoreporter.database.entity.type.ReportStatus.POST_CREATED;
import static com.marteczek.photoreporter.database.entity.type.ReportStatus.PUBLISHED;
import static com.marteczek.photoreporter.database.entity.type.ReportStatus.SENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReportServiceTest {

    @Mock
    private ReportDao reportDao;

    @Mock
    private ItemDao itemDao;

    @Mock
    private PostDao postDao;

    @Mock
    PictureManager pictureManager;

    private ReportDatabaseHelper reportDatabaseHelper = new ReportDatabaseHelperTestImpl();

    private MainThreadRunner mainThreadRunner = new MainThreadRunnerTestImpl();

    @Mock
    OnErrorListener onErrorListener;

    @Mock
    ReportService.OnFinishListener onFinishListener;

    @Test
    public void insertItemsToReport_exceptionIsThrown_errorCallbackFunctionIsCalled() {
        //given
        ReportService service = new ReportService(reportDao, itemDao, postDao, pictureManager,
                reportDatabaseHelper, mainThreadRunner);
        when(reportDao.findById(any())).thenThrow(new SQLiteException());
        List<PictureItem> pictures = new ArrayList<>();
        //when
        service.insertItemsToReport(1L, pictures, 200, null, onErrorListener);
        //then
        verify(onErrorListener, times(1)).onError(any());
        verifyNoMoreInteractions(onErrorListener);
    }

    @Test
    public void insertItemsToReport_methodExecutedSuccessfully_onFinishIsCalled() {
        //given
        ReportService service = new ReportService(reportDao, itemDao, postDao, pictureManager,
                reportDatabaseHelper, mainThreadRunner);
        Report report = Report.builder().id(1L).name("R1").status(ReportStatus.NEW).build();
        when(reportDao.findById(any())).thenReturn(report);
        List<PictureItem> pictures = new ArrayList<>();
        //when
        service.insertItemsToReport(1L, pictures, 200, onFinishListener, null);
        //then
        verify(onFinishListener, times(1)).onFinish(any());
        verifyNoMoreInteractions(onErrorListener);
    }

    @Test
    public void insertItemsToReport_reportHasNotNewStatus_IllegalStateExceptionIsThrown() {
        //given
        String[] statuses = new String[] {PENDING, SENT, PICTURE_SENDING_FAILURE,
                PICTURE_SENDING_CANCELLED, POST_CREATED, PUBLISHED};
        int numberOfReports = statuses.length;
        List<Report> reports = new ArrayList<>();
        long id = 0L;
        for (String status : statuses) {
            reports.add(Report.builder().id(id).name("R" + id).status(status).build());
            id++;
        }
        ReportService service = new ReportService(reportDao, itemDao, postDao, pictureManager,
                reportDatabaseHelper, mainThreadRunner);
        List<PictureItem> pictures = new ArrayList<>();
        when(reportDao.findById(any())).thenAnswer(
                invocation -> reports.get(invocation.getArgument(0, Long.class).intValue()));
        //when
        for(id = 0L; id < numberOfReports; id++) {
            service.insertItemsToReport(1L, pictures, 200, null, onErrorListener);
        }
        //then
        verify(onErrorListener, times(numberOfReports)).onError(any(IllegalStateException.class));
        verifyNoMoreInteractions(onErrorListener);
    }
}