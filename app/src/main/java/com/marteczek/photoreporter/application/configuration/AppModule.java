package com.marteczek.photoreporter.application.configuration;

import android.app.Application;

import com.marteczek.photoreporter.application.PhotoReporterApplication;
import com.marteczek.photoreporter.ui.FindThreadActivity;
import com.marteczek.photoreporter.ui.PicturePreviewActivity;
import com.marteczek.photoreporter.ui.PostActivity;
import com.marteczek.photoreporter.ui.PostAllActivity;
import com.marteczek.photoreporter.ui.PostOpenInBrowserActivity;
import com.marteczek.photoreporter.ui.ReportActivity;
import com.marteczek.photoreporter.ui.ReportListActivity;
import com.marteczek.photoreporter.ui.SettingsActivity;
import com.marteczek.photoreporter.ui.ThreadListActivity;


import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
abstract class AppModule {

    @Binds
    abstract Application application(PhotoReporterApplication application);

    @ContributesAndroidInjector
    abstract FindThreadActivity findThreadActivity();

    @ContributesAndroidInjector
    abstract PicturePreviewActivity picturePreviewActivity();

    @ContributesAndroidInjector
    abstract PostActivity postActivity();

    @ContributesAndroidInjector
    abstract PostAllActivity postInBrowserActivity();

    @ContributesAndroidInjector
    abstract PostOpenInBrowserActivity postOpenInBrowserActivity();

    @ContributesAndroidInjector
    abstract ReportListActivity reportListActivity();

    @ContributesAndroidInjector
    abstract ReportActivity reportActivity();

    @ContributesAndroidInjector
    abstract SettingsActivity settingsActivity();

    @ContributesAndroidInjector
    abstract SettingsActivity.SettingsWatermarkFragment settingsWatermarkFragment();

    @ContributesAndroidInjector
    abstract ThreadListActivity threadListActivity();

}