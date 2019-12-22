package com.marteczek.photoreporter.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.service.ReportService;

public class PostOpenInBrowserViewModel extends AndroidViewModel {

    private final ReportService reportService;

    private LiveData<Report> report;

    public PostOpenInBrowserViewModel(@NonNull Application application, ReportService reportService) {
        super(application);
        this.reportService = reportService;
    }

    LiveData<Report> getReport(Long reportId) {
        if (report == null) {
            report = reportService.findReportByIdAsync(reportId);
        }
        return report;
    }
}
