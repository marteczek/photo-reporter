package com.marteczek.photoreporter.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.marteczek.photoreporter.database.entity.ForumThread;
import com.marteczek.photoreporter.service.ThreadService;

import java.util.List;

public class ThreadListViewModel extends AndroidViewModel {

    private LiveData<List<ForumThread>> threads;

    public ThreadListViewModel(@NonNull Application application, ThreadService threadService) {
        super(application);
        threads = threadService.findThreadsOrderByName();
    }

    LiveData<List<ForumThread>> getThreads() {
        return threads;
    }
}
