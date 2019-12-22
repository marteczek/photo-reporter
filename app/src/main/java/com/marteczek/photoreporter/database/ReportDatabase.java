package com.marteczek.photoreporter.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.marteczek.photoreporter.database.dao.ItemDao;
import com.marteczek.photoreporter.database.dao.PostDao;
import com.marteczek.photoreporter.database.dao.ReportDao;
import com.marteczek.photoreporter.database.dao.ThreadDao;
import com.marteczek.photoreporter.database.entity.ForumThread;
import com.marteczek.photoreporter.database.entity.Item;
import com.marteczek.photoreporter.database.entity.Post;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.database.entity.converter.Converter;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {ForumThread.class, Item.class, Post.class, Report.class}, version = 1,
        exportSchema = false)
@TypeConverters(Converter.class)
public abstract class ReportDatabase extends RoomDatabase {

    private static volatile ReportDatabase INSTANCE;

    private static final int NUMBER_OF_THREADS = 4;

    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);

            databaseWriteExecutor.execute(() -> {
                ThreadDao threadDao = INSTANCE.threadDao();
                ForumThread thread = threadDao.findOne();
                if (thread == null) {
                    String threadId = "2174520";
                    String name = "Fotorelacjonusz tests";
                    thread = ForumThread.builder().threadId(threadId).name(name)
                            .lastUsage(new Date()).build();
                    threadDao.insert(thread);
                }
            });
        }
    };

    public static ReportDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized ( (ReportDatabase.class)) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ReportDatabase.class, "report_database")
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract ItemDao itemDao();

    public abstract ReportDao reportDao();

    public abstract ThreadDao threadDao();

    public abstract PostDao postDao();
}
