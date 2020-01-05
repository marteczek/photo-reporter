package com.marteczek.photoreporter.service;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.marteczek.photoreporter.database.ReportDatabaseHelper;
import com.marteczek.photoreporter.database.dao.ThreadDao;
import com.marteczek.photoreporter.database.entity.ForumThread;

import java.util.List;

import static com.marteczek.photoreporter.application.Settings.Debug.E;

public class ThreadService {

    private static final String TAG = "BaseService";

    private final ThreadDao threadDao;

    private final ReportDatabaseHelper dbHelper;

    public ThreadService(ThreadDao threadDao, ReportDatabaseHelper reportDatabaseHelper) {
        this.threadDao = threadDao;
        this.dbHelper = reportDatabaseHelper;
    }

    public LiveData<List<ForumThread>> findThreadsOrderByName() {
        return threadDao.findAllOrderByNameAsync();
    }

    public LiveData<ForumThread> findThreadByThreadId(String threadId) {
        return threadDao.findByThreadIdAsync(threadId);
    }

    public void saveThread(ForumThread thread, OnErrorListener onErrorListener) {
        dbHelper.executeInTransaction(() -> {
            try {
                String threadId = thread.getThreadId();
                if (threadDao.findByThreadId(threadId) == null) {
                    threadDao.insert(thread);
                } else {
                    threadDao.updateNameByThreadId(threadId, thread.getName());
                    threadDao.updateLastUsageByThreadId(threadId, thread.getLastUsage());
                }
            } catch (RuntimeException e) {
                if(E) Log.e(TAG, "RuntimeException", e);
                if (onErrorListener != null) {
                    onErrorListener.onError(e);
                }
            }
        });
    }

    public void deleteThread(String threadId) {
        dbHelper.execute(() -> threadDao.deleteByThreadId(threadId));
    }
}
