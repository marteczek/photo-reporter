package com.marteczek.photoreporter.application.configuration;

import android.app.Application;

import com.marteczek.photoreporter.application.MainThreadRunner;
import com.marteczek.photoreporter.application.MainThreadRunnerImpl;
import com.marteczek.photoreporter.database.ReportDatabaseHelper;
import com.marteczek.photoreporter.database.dao.ItemDao;
import com.marteczek.photoreporter.database.dao.PostDao;
import com.marteczek.photoreporter.database.dao.ReportDao;
import com.marteczek.photoreporter.database.dao.ThreadDao;
import com.marteczek.photoreporter.picturemanager.PictureManager;
import com.marteczek.photoreporter.service.ItemService;
import com.marteczek.photoreporter.service.PostService;
import com.marteczek.photoreporter.service.ReportService;
import com.marteczek.photoreporter.service.ThreadService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
class ServiceModule {

    @Singleton
    @Provides
    MainThreadRunner mainThreadRunner(Application application) {
        return new MainThreadRunnerImpl(application.getApplicationContext());
    }

    @Singleton
    @Provides
    ItemService itemService(ItemDao itemDao, PictureManager pictureManager,
                            ReportDatabaseHelper reportDatabaseHelper,
                            MainThreadRunner mainThreadRunner){
        return new ItemService(itemDao, pictureManager, reportDatabaseHelper, mainThreadRunner);
    }

    @Singleton
    @Provides
    PostService postService(PostDao postDao, ItemDao itemDao, ReportDao reportDao,
                            ReportDatabaseHelper reportDatabaseHelper){
        return new PostService(postDao, itemDao, reportDao, reportDatabaseHelper);
    }

    @Singleton
    @Provides
    ReportService reportService(ReportDao reportDao, ItemDao itemDao, PostDao postDao,
                                PictureManager pictureManager, ReportDatabaseHelper reportDatabaseHelper,
                                MainThreadRunner mainThreadRunner) {
        return new ReportService(reportDao, itemDao, postDao, pictureManager, reportDatabaseHelper,
                mainThreadRunner);
    }

    @Singleton
    @Provides
    ThreadService threadService(ThreadDao threadDao, ReportDatabaseHelper reportDatabaseHelper) {
        return new ThreadService(threadDao, reportDatabaseHelper);
    }
}
