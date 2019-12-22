package com.marteczek.photoreporter.service.baseservice;

import android.content.Context;

import com.marteczek.photoreporter.database.ReportDatabase;

public class BaseService {

    protected ReportDatabase db;

    protected Context context;

    public interface OnErrorListener {
        void onError(RuntimeException e);
    }

    public BaseService(final Context context) {
        db = ReportDatabase.getDatabase(context);
        this.context = context.getApplicationContext();
    }

    protected void executeInTransaction(Runnable body) {
        ReportDatabase.databaseWriteExecutor.execute(
                () -> db.runInTransaction(body));
    }

}
