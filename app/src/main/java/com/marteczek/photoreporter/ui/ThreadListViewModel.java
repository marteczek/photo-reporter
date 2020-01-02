package com.marteczek.photoreporter.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.marteczek.photoreporter.database.entity.ForumThread;
import com.marteczek.photoreporter.service.ReportService;
import com.marteczek.photoreporter.service.ThreadService;

import java.util.List;

public class ThreadListViewModel extends AndroidViewModel {

    private final ThreadService threadService;

    private LiveData<List<ForumThread>> threads;

    private LiveData<List<String>> threadsIdsInReports;

    public ThreadListViewModel(@NonNull Application application, ThreadService threadService,
                               ReportService reportService) {
        super(application);
        this.threadService = threadService;
        threads = threadService.findThreadsOrderByName();
        threadsIdsInReports = reportService.findThreadsIds();
    }

    LiveData<List<ForumThread>> getThreads() {
        return threads;
    }

    LiveData<List<String>> getThreadsIdsInReports() {
        return threadsIdsInReports;
    }

    void removeThread(String threadId) {
        threadService.deleteThread(threadId);
    }
}
