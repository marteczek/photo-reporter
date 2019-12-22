package com.marteczek.photoreporter.ui;

import android.app.Application;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marteczek.photoreporter.database.entity.Post;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.service.PostService;
import com.marteczek.photoreporter.service.ReportService;

import java.util.List;

import static com.marteczek.photoreporter.application.Settings.Debug.D;

public class PostAllViewModel extends AndroidViewModel {
    private static final String TAG = "PostAllViewModel";

    private final PostService postService;

    private final ReportService reportService;

    private final Handler handler;

    private LiveData<Report> report;

    private LiveData<List<Post>> posts;

    private MutableLiveData<Integer> timer;

    private int postNumber = 1;

    private boolean sending = false;

    private Runnable wait = new Runnable() {
        @Override
        public void run() {
            if (timer.getValue() != null) {
                int t = timer.getValue();
                if (t > 0) {
                    timer.setValue(t - 1);
                    handler.postDelayed(this, 1000);
                }
            }
            if(D) Log.d(TAG, "Waiting: " + timer.getValue());
        }
    };

    public PostAllViewModel(@NonNull Application application, PostService postService, ReportService reportService) {
        super(application);
        this.postService = postService;
        this.reportService = reportService;
        handler = new Handler(getApplication().getMainLooper());
    }

    LiveData<Report> getReport(Long reportId) {
        if (report == null) {
            report = reportService.findReportByIdAsync(reportId);
        }
        return report;
    }

    LiveData<List<Post>> getPosts(Long reportId) {
        if (posts == null) {
            posts = postService.findPostsByReportId(reportId);
        }
        return posts;
    }

    LiveData<Integer> getTimer() {
        if (timer == null) {
            timer = new MutableLiveData<>();
        }
        return timer;
    }

    int getPostNumber() {
        return postNumber;
    }

    void setPostNumber(int postNumber) {
        this.postNumber = postNumber;
    }

    boolean isSending() {
        return sending;
    }

    void setSending(boolean sending) {
        this.sending = sending;
    }

    void startTimer(int time) {
        timer.setValue(time);
        handler.postDelayed(wait, 1000);
    }

    void stopTimer() {
        handler.removeCallbacks(wait);
    }
}
