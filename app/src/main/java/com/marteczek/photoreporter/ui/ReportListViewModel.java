package com.marteczek.photoreporter.ui;

import android.app.Application;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.Settings;
import com.marteczek.photoreporter.application.background.ThumbnailWorker;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.service.ReportService;
import com.marteczek.photoreporter.service.data.PictureItem;

import java.util.Collections;
import java.util.List;

public class ReportListViewModel extends AndroidViewModel {

    private ReportService reportService;

    private LiveData<List<Report>> reports;

    public ReportListViewModel(Application application, ReportService reportService) {
        super(application);
        this.reportService = reportService;
        reports = reportService.findReportsOrderedByDateDesc();
    }

    LiveData<List<Report>> getReports() {
        return reports;
    }

    void insertReportWithItems(Report report, List<PictureItem> pictures) {
        switch(Settings.getPictureOrder(getApplication())) {
            case "last_modified_asc":
                Collections.sort(pictures, (first, second) -> (int)
                        ((first.getLastModified().getTime() - second.getLastModified().getTime()) / 1000));
                break;
            case "last_modified_desc":
                Collections.sort(pictures, (first, second) -> (int)
                        ((second.getLastModified().getTime() - first.getLastModified().getTime()) / 1000));
                break;
        }
        reportService.insertReportWithItems(report, pictures,
                Settings.getThumbnailDimension(getApplication()),
                reportId -> {
            Data data = new Data.Builder()
                    .putLong(ThumbnailWorker.DATA_REPORT_ID, reportId)
                    .build();
            OneTimeWorkRequest thumbnailWorkRequest =
                    new OneTimeWorkRequest.Builder(ThumbnailWorker.class)
                    .setInputData(data)
                    .build();
            WorkManager.getInstance(getApplication()).enqueueUniqueWork(ThumbnailWorker.NAME,
                    ExistingWorkPolicy.REPLACE, thumbnailWorkRequest);
        }, e -> Toast.makeText(getApplication(), R.string.database_error, Toast.LENGTH_LONG).show());
    }

    void removeReport(Report report) {
        reportService.deleteReportWithDependencies(report.getId(),
                e -> Toast.makeText(getApplication(), R.string.database_error, Toast.LENGTH_LONG).show() );
    }
}
