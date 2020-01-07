package com.marteczek.photoreporter.application;

import android.content.Context;
import android.os.Handler;


public class MainThreadRunnerImpl implements MainThreadRunner {
    private final Handler handler;

    public MainThreadRunnerImpl(Context context) {
        this.handler = new Handler(context.getMainLooper());
    }

    @Override
    public void run(Runnable runnable) {
        handler.post(runnable);
    }
}
