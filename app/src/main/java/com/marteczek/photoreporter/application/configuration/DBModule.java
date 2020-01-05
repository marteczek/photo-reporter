package com.marteczek.photoreporter.application.configuration;

import android.app.Application;

import com.marteczek.photoreporter.database.ReportDatabase;
import com.marteczek.photoreporter.database.ReportDatabaseHelper;
import com.marteczek.photoreporter.database.ReportDatabaseHelperImpl;
import com.marteczek.photoreporter.database.dao.ItemDao;
import com.marteczek.photoreporter.database.dao.PostDao;
import com.marteczek.photoreporter.database.dao.ReportDao;
import com.marteczek.photoreporter.database.dao.ThreadDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
class DBModule {

    @Singleton
    @Provides
    ReportDatabase reportDatabase(Application application) {
        return ReportDatabase.getDatabase(application.getApplicationContext());
    }

    @Singleton
    @Provides
    ReportDatabaseHelper reportDatabaseHelper(Application application) {
        return new ReportDatabaseHelperImpl(application.getApplicationContext());
    }

    @Singleton
    @Provides
    ReportDao reportDao(ReportDatabase reportDatabase) {
        return reportDatabase.reportDao();
    }

    @Singleton
    @Provides
    ItemDao itemDao(ReportDatabase reportDatabase) {
        return reportDatabase.itemDao();
    }

    @Singleton
    @Provides
    PostDao postDao(ReportDatabase reportDatabase) {
        return reportDatabase.postDao();
    }

    @Singleton
    @Provides
    ThreadDao threadDao(ReportDatabase reportDatabase) {
        return reportDatabase.threadDao();
    }
}
