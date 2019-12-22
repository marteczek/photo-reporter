package com.marteczek.photoreporter.application.configuration;

import com.marteczek.photoreporter.application.PhotoReporterApplication;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;

@Singleton
@Component(modules = {AndroidInjectionModule.class, AppModule.class, ViewModelModule.class,
        DBModule.class, ServiceModule.class, PictureManagerModule.class})
public interface AppComponent extends AndroidInjector<PhotoReporterApplication> {

    @Component.Factory
    interface Factory extends AndroidInjector.Factory<PhotoReporterApplication> {
    }
}
