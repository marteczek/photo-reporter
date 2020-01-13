package com.marteczek.photoreporter.service;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.marteczek.photoreporter.database.ReportDatabase;
import com.marteczek.photoreporter.database.ReportDatabaseHelper;
import com.marteczek.photoreporter.database.dao.ThreadDao;
import com.marteczek.photoreporter.database.entity.ForumThread;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class ThreadServiceTest {

    private ThreadDao threadDao;

    private ReportDatabaseHelper reportDatabaseHelper = new ReportDatabaseHelperTestImpl();

    private ReportDatabase db;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, ReportDatabase.class).build();
        threadDao = db.threadDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void saveThread_threadDoesNotExist_threadIsInserted() {
        //given
        ThreadService service = new ThreadService(threadDao, reportDatabaseHelper);
        String threadId = "1";
        ForumThread thread = ForumThread.builder().threadId(threadId).build();
        //when
        service.saveThread(thread, null);
        //then
        thread = threadDao.findByThreadId(threadId);
        assertNotNull(thread);
    }

    @Test
    public void saveThread_threadExists_threadIsUpdated() {
        //given
        ThreadService service = new ThreadService(threadDao, reportDatabaseHelper);
        String threadId = "1";
        ForumThread thread = ForumThread.builder().threadId(threadId).build();
        threadDao.insert(thread);
        String name = "name";
        Date date = new Date(0L);
        String path = "path";
        thread = ForumThread.builder().threadId(threadId).name(name).lastUsage(date).threadPath(path).build();
        //when
        service.saveThread(thread, null);
        //then
        thread = threadDao.findByThreadId(threadId);
        assertNotNull(thread);
        assertEquals(name, thread.getName());
        assertEquals(date, thread.getLastUsage());
        assertEquals(path, thread.getThreadPath());
    }
}