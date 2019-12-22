package com.marteczek.photoreporter.service;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.marteczek.photoreporter.database.dao.ThreadDao;
import com.marteczek.photoreporter.database.entity.ForumThread;
import com.marteczek.photoreporter.service.baseservice.BaseService;

import java.util.List;

public class ThreadService extends BaseService {

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
                e.printStackTrace();
                if (onErrorListener != null) {
                    onErrorListener.onError(e);
                }
            }
        });
    }
}
