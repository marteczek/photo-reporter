package com.marteczek.photoreporter.service;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.marteczek.photoreporter.database.ReportDatabase;
import com.marteczek.photoreporter.database.dao.ThreadDao;
import com.marteczek.photoreporter.database.entity.ForumThread;
import com.marteczek.photoreporter.service.baseservice.BaseService;

import java.util.List;

import static com.marteczek.photoreporter.application.Settings.Debug.E;

public class ThreadService extends BaseService {

    private static final String TAG = "BaseService";
    private ThreadDao threadDao;

    public ThreadService(Application application, ThreadDao threadDao) {
        super(application);
        this.threadDao = threadDao;
    }

    public LiveData<List<ForumThread>> findThreadsOrderByName() {
        return threadDao.findAllOrderByNameAsync();
    }

    public LiveData<ForumThread> findThreadByThreadId(String threadId) {
        return threadDao.findByThreadIdAsync(threadId);
    }

    public void saveThread(ForumThread thread, OnErrorListener onErrorListener) {
        executeInTransaction(() -> {
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
        ReportDatabase.databaseWriteExecutor.execute(() -> threadDao.deleteByThreadId(threadId));
    }
}
