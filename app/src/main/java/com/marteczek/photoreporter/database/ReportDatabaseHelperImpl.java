package com.marteczek.photoreporter.database;

import android.content.Context;

public class ReportDatabaseHelperImpl implements ReportDatabaseHelper {

    private final ReportDatabase db;

    public ReportDatabaseHelperImpl(Context context) {
        db = ReportDatabase.getDatabase(context);
    }

    @Override
    public void execute(Runnable body) {
        ReportDatabase.databaseWriteExecutor.execute(body);
    }

    @Override
    public void executeInTransaction(Runnable body) {
        ReportDatabase.databaseWriteExecutor.execute(
                () -> db.runInTransaction(body));
    }
}
