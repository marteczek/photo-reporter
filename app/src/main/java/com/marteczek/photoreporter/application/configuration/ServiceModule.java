package com.marteczek.photoreporter.application.configuration;

import android.app.Application;

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
    ItemService itemService(Application application, ItemDao itemDao, PictureManager pictureManager){
        return new ItemService(application, itemDao, pictureManager);
    }

    @Singleton
    @Provides
    PostService postService(Application application, PostDao postDao, ItemDao itemDao, ReportDao reportDao){
        return new PostService(application, postDao, itemDao, reportDao);
    }

    @Singleton
    @Provides
    ReportService reportService(Application application, ReportDao reportDao, ItemDao itemDao,
                                PostDao postDao, PictureManager pictureManager) {
        return new ReportService(application.getApplicationContext(), reportDao, itemDao, postDao,
                pictureManager);
    }

    @Singleton
    @Provides
    ThreadService threadService(Application application, ThreadDao threadDao) {
        return new ThreadService(application, threadDao);
    }
}
