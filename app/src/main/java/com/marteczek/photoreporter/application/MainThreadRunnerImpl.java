package com.marteczek.photoreporter.application;

import android.app.Application;
import android.os.Handler;


public class MainThreadRunnerImpl implements MainThreadRunner {
    private final Handler handler;

    public MainThreadRunnerImpl(Application application) {
        this.handler = new Handler(application.getMainLooper());
    }

    @Override
    public void run(Runnable runnable) {
        handler.post(runnable);
    }
}
