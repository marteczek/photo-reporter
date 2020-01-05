package com.marteczek.photoreporter.database;

public interface ReportDatabaseHelper {

    void execute(Runnable body);

    void executeInTransaction(Runnable body);
}
