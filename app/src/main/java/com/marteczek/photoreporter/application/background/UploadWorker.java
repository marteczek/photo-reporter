package com.marteczek.photoreporter.application.background;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.impl.foreground.SystemForegroundDispatcher;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.Settings;
import com.marteczek.photoreporter.application.data.ImgurUserData;
import com.marteczek.photoreporter.database.ReportDatabase;
import com.marteczek.photoreporter.picturehostclient.BaseResponse;
import com.marteczek.photoreporter.picturehostclient.ImageHostClient;
import com.marteczek.photoreporter.picturehostclient.imgur.ImgurAccountClient;
import com.marteczek.photoreporter.picturehostclient.imgur.ImgurAnonymousClient;
import com.marteczek.photoreporter.picturehostclient.imgur.ImgurResponse;
import com.marteczek.photoreporter.picturehostclient.testclient.TestClient;
import com.marteczek.photoreporter.picturemanager.PictureManagerImpl;
import com.marteczek.photoreporter.service.misc.PictureFormat;
import com.marteczek.photoreporter.service.UploadImagesService;

import static com.marteczek.photoreporter.application.Settings.Debug.TEST_UPLOAD_CLIENT_ENABLED;

public class UploadWorker extends Worker implements UploadImagesService.UploadManager {
    public static final String DATA_REPORT_ID = "data_report_id";
    public static final String NAME = "upload_work";
    public static final String DATA_PROGRESS = "data_progress";
    public static final String DATA_MAX_PROGRESS = "data_max_progress";

    private NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
    private final Handler handler;

    public UploadWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        handler = new Handler(context.getMainLooper());
    }

    @Override
    public @NonNull
    Result doWork() {
        Context context = getApplicationContext();
        String pictureHost = Settings.getPictureHost(context);
        if ("imgur_account".equals(pictureHost)) {
            ImgurUserData userData = Settings.getImgurUserData(context);
            if (userData != null) {
                ImageHostClient client;
                if (TEST_UPLOAD_CLIENT_ENABLED) {
                    client = new TestClient();
                } else {
                    client = new ImgurAccountClient(context);
                }
                UploadImagesService.Response response = uploadImages(context, client);
                BaseResponse clientResponse = response.getClientResponse();
                if (clientResponse instanceof ImgurResponse) {
                    Boolean isEnoughCredits = ((ImgurResponse) clientResponse).getEnoughCredits();
                    if (isEnoughCredits != null && !isEnoughCredits) {
                        handler.post(() -> Toast.makeText(context, R.string.imgur_not_enough_credits,
                                Toast.LENGTH_LONG).show());

                    }
                    Boolean isLoggedIn = ((ImgurResponse) clientResponse).getLoggedIn();
                    if(isLoggedIn != null && !isLoggedIn) {
                        handler.post(() -> Toast.makeText(context, R.string.not_logged, Toast.LENGTH_LONG).show());
                    }
                }
            } else {
                handler.post(() -> Toast.makeText(context, R.string.not_logged, Toast.LENGTH_LONG).show());
            }
        }
        if ("imgur_anonymous".equals(pictureHost)) {
            ImageHostClient client = new ImgurAnonymousClient(context);
            UploadImagesService.Response response = uploadImages(context, client);
            BaseResponse clientResponse = response.getClientResponse();
            if (clientResponse instanceof ImgurResponse) {
                Boolean isEnoughCredits = ((ImgurResponse) clientResponse).getEnoughCredits();
                if (isEnoughCredits != null && !isEnoughCredits) {
                    handler.post(() -> Toast.makeText(context, R.string.imgur_not_enough_credits,
                            Toast.LENGTH_LONG).show());
                }
            }
        }
        return Result.success();
    }

    private UploadImagesService.Response uploadImages(Context context, ImageHostClient client) {
        PendingIntent intent = WorkManager.getInstance(context)
                .createCancelPendingIntent(getId());
        NotificationCompat.Builder notificationBuilder;
        notificationBuilder = new NotificationCompat.Builder(context, Settings.CHANNEL_UPLOAD_ID)
                .setContentTitle(context.getString(R.string.notification_upload_title))
                .setContentText(context.getString(R.string.notification_upload_text))
                .setSmallIcon(R.drawable.ic_cloud_upload_white_24dp)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_delete,
                        context.getString(R.string.notification_upload_cancel), intent);
        setForegroundAsync(new ForegroundInfo(notificationBuilder.build()));
        ReportDatabase db = ReportDatabase.getDatabase(context);
        UploadImagesService uploadImagesService = new UploadImagesService(new PictureManagerImpl(context),
                db.reportDao(), db.itemDao());
        Long reportId = getInputData().getLong(DATA_REPORT_ID, 0);
        int dimension = Settings.getPictureDimension(context);
        PictureFormat pictureFormat = PictureFormat.builder().greaterDimension(dimension).build();
        UploadImagesService.Response result = uploadImagesService.uploadImages(reportId, client,
                pictureFormat, this,
                (max, current) -> {
                    notificationBuilder.setProgress(max, current, false).build();

                    //TODO unofficial; check future versions of the library for a better solution
                    notificationManager.notify(SystemForegroundDispatcher.NOTIFICATION_ID,
                            notificationBuilder.build());
                    setProgressAsync(new Data.Builder()
                            .putInt(DATA_PROGRESS, current)
                            .putInt(DATA_MAX_PROGRESS, max)
                            .build());
                });
        if (result.isSucceeded()) {
            handler.post(() -> Toast.makeText(context, R.string.pictures_uploaded_successfully,
                    Toast.LENGTH_LONG).show());
        } else {
            handler.post(() -> Toast.makeText(context, R.string.pictures_upload_error,
                    Toast.LENGTH_LONG).show());
        }
        return result;
    }
}