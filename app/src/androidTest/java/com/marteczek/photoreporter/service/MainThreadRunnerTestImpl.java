package com.marteczek.photoreporter.service;

import com.marteczek.photoreporter.application.MainThreadRunner;

public class MainThreadRunnerTestImpl implements MainThreadRunner {

    @Override
    public void run(Runnable runnable) {
        runnable.run();
    }
}