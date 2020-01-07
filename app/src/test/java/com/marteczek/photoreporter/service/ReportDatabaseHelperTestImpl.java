package com.marteczek.photoreporter.service;

import com.marteczek.photoreporter.database.ReportDatabaseHelper;

public class ReportDatabaseHelperTestImpl implements ReportDatabaseHelper {
    @Override
    public void execute(Runnable body) {
        body.run();
    }

    @Override
    public void executeInTransaction(Runnable body) {
        body.run();
    }
}
