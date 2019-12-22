package com.marteczek.photoreporter.application.background;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.service.ItemService;

public class ThumbnailWorker extends Worker {
    public static final String DATA_REPORT_ID = "data_report_id";
    public static final String NAME = "thumbnail_work";

    private ItemService itemService;

    public ThumbnailWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        itemService = new ItemService(getApplicationContext());
    }

    @Override
    public @NonNull Result doWork() {
        Long reportId = getInputData().getLong(DATA_REPORT_ID, 0);
        itemService.createThumbnails(reportId, e -> {
            Handler handler = new Handler(getApplicationContext().getMainLooper());
            handler.post(() -> Toast.makeText(getApplicationContext(), R.string.database_error,
                    Toast.LENGTH_LONG).show());
        });
        return Result.success();
    }
}