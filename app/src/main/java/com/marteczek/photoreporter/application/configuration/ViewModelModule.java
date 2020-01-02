package com.marteczek.photoreporter.application.configuration;

import android.app.Application;

import androidx.lifecycle.ViewModel;

import com.marteczek.photoreporter.application.configuration.viewmodelfactory.ViewModelFactory;
import com.marteczek.photoreporter.picturemanager.PictureManager;
import com.marteczek.photoreporter.service.ItemService;
import com.marteczek.photoreporter.service.PostService;
import com.marteczek.photoreporter.service.ReportService;
import com.marteczek.photoreporter.service.ThreadService;
import com.marteczek.photoreporter.ui.PicturePreviewViewModel;
import com.marteczek.photoreporter.ui.PostAllViewModel;
import com.marteczek.photoreporter.ui.PostOpenInBrowserViewModel;
import com.marteczek.photoreporter.ui.PostViewModel;
import com.marteczek.photoreporter.ui.ReportListViewModel;
import com.marteczek.photoreporter.ui.ReportViewModel;
import com.marteczek.photoreporter.ui.SettingsWatermarkViewModel;
import com.marteczek.photoreporter.ui.ThreadListViewModel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import javax.inject.Provider;

import dagger.MapKey;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;

@Module
class ViewModelModule {

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @MapKey
    @interface ViewModelKey {
        Class<? extends ViewModel> value();
    }

    @Provides
    ViewModelFactory viewModelFactory(Map<Class<? extends ViewModel>, Provider<ViewModel>> providerMap) {
        return new ViewModelFactory(providerMap);
    }

    @Provides
    @IntoMap
    @ViewModelKey(PicturePreviewViewModel.class)
    ViewModel picturePreviewViewModel(Application application, PictureManager pictureManager) {
        return new PicturePreviewViewModel(application, pictureManager);
    }

    @Provides
    @IntoMap
    @ViewModelKey(PostViewModel.class)
    ViewModel postViewModel(Application application, PostService postService) {
        return new PostViewModel(application, postService);
    }

    @Provides
    @IntoMap
    @ViewModelKey(PostAllViewModel.class)
    ViewModel postAllViewModel(Application application, PostService postService, ReportService reportService) {
        return new PostAllViewModel(application, postService, reportService);
    }

    @Provides
    @IntoMap
    @ViewModelKey(PostOpenInBrowserViewModel.class)
    ViewModel postOpenInBrowserViewModel(Application application, ReportService reportService) {
        return new PostOpenInBrowserViewModel(application, reportService);
    }

    @Provides
    @IntoMap
    @ViewModelKey(ReportListViewModel.class)
    ViewModel reportListViewModel(Application application, ReportService reportService) {
        return new ReportListViewModel(application, reportService);
    }

    @Provides
    @IntoMap
    @ViewModelKey(ReportViewModel.class)
    ViewModel reportViewModel(Application application,
                              ReportService reportService, PostService postService,
                              ItemService itemService, ThreadService threadService) {
        return new ReportViewModel(application, reportService, postService,
                itemService, threadService);
    }

    @Provides
    @IntoMap
    @ViewModelKey(SettingsWatermarkViewModel.class)
    ViewModel settingsViewModel(Application application, PictureManager pictureManager) {
        return new SettingsWatermarkViewModel(application, pictureManager);
    }

    @Provides
    @IntoMap
    @ViewModelKey(ThreadListViewModel.class)
    ViewModel threadListViewModel(Application application, ThreadService threadService,
                                  ReportService reportService) {
        return new ThreadListViewModel(application, threadService, reportService);
    }
}

