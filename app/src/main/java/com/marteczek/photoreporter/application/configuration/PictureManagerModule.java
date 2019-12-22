package com.marteczek.photoreporter.application.configuration;

import android.app.Application;

import com.marteczek.photoreporter.picturemanager.PictureManager;
import com.marteczek.photoreporter.picturemanager.PictureManagerImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
class PictureManagerModule {

    @Singleton
    @Provides
    PictureManager pictureManager(Application application) {
        return new PictureManagerImpl(application);
    }
}
