package com.marteczek.photoreporter.service;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.marteczek.photoreporter.database.ReportDatabase;
import com.marteczek.photoreporter.database.dao.ItemDao;
import com.marteczek.photoreporter.database.dao.PostDao;
import com.marteczek.photoreporter.database.dao.ReportDao;
import com.marteczek.photoreporter.database.entity.Item;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.picturemanager.PictureManager;
import com.marteczek.photoreporter.imagetools.ImageUtils;
import com.marteczek.photoreporter.service.baseservice.BaseService;
import com.marteczek.photoreporter.service.data.PictureItem;

import java.io.IOException;
import java.util.List;

import static com.marteczek.photoreporter.application.Settings.Debug.E;

public class ReportService extends BaseService {
    private final static String TAG = "ReportService";

    @FunctionalInterface
    public interface OnFinishedListener {
        void onFinished(Long reportId);
    }

    private ItemDao itemDao;

    private PostDao postDao;

    private ReportDao reportDao;

    private PictureManager pictureManager;

    public ReportService(final Context context, ReportDao reportDao, ItemDao itemDao,
                         PostDao postDao, PictureManager pictureManager) {
        super(context);
        this.reportDao = reportDao;
        this.itemDao = itemDao;
        this.postDao = postDao;
        this.pictureManager = pictureManager;
    }

    public LiveData<Report> findReportByIdAsync(Long id) {
        return reportDao.findByIdAsync(id);
    }

    public LiveData<List<Report>> findReportsOrderedByDateDesc() {
        return reportDao.findAllOrderByDateDescAsync();
    }

    public void insertReportWithItems(final Report report, final List<PictureItem> imageItems,
                                      int thumbnailDimension, OnFinishedListener onFinishedListener,
                                      OnErrorListener onErrorListener) {
        executeInTransaction(() -> {
            try {
                long reportId = reportDao.insert(report);
                long succession = 0L;

                for (PictureItem pictureItem : imageItems) {
                    Long itemId = itemDao.insert(Item.builder()
                            .reportId(reportId)
                            .thumbnailRequiredWidth(thumbnailDimension)
                            .thumbnailRequiredHeight(thumbnailDimension)
                            .status("new")
                            .build());
                    try {
                        Uri uri = pictureItem.getPictureUri();
                        String path = pictureManager.copy(uri, "picture" + itemId);
                        itemDao.updatePicturePathById(itemId, path);
                        itemDao.updatePictureRotationById(itemId, ImageUtils.getImageRotation(path));
                        itemDao.updateSuccessionById(itemId, succession++);
                    } catch (IOException e) {
                        if(E) Log.e(TAG, "IOException", e);
                        itemDao.deleteById(itemId);
                    }
                }
                if (onFinishedListener != null) {
                    Handler handler = new Handler(context.getMainLooper());
                    handler.post(() -> onFinishedListener.onFinished(reportId));
                }
            } catch (RuntimeException e) {
                if(E) Log.e(TAG, "RuntimeException", e);
                if (onErrorListener != null) {
                    onErrorListener.onError(e);
                }
            }
        });
    }

    public void updateReportName(Long id, String name) {
        ReportDatabase.databaseWriteExecutor.execute(() -> reportDao.updateNameById(id, name));
    }

    public void updateThreadId(Long id, String newThreadId) {
        ReportDatabase.databaseWriteExecutor.execute(() ->
                reportDao.updateThreadId(id, newThreadId));
    }

    public void deleteReportWithDependencies(Long reportId, OnErrorListener onErrorListener) {
        executeInTransaction(() -> {
            try {
                List<Item> items = itemDao.findByReportId(reportId);
                itemDao.deleteByReportId(reportId);
                postDao.deleteByReportId(reportId);
                reportDao.deleteById(reportId);
                for (Item item : items) {
                    String path = item.getPicturePath();
                    boolean result;
                    if (path != null) {
                        result = pictureManager.deleteFile(path);
                        if (!result) {
                            if(E) Log.e(TAG, "File not deleted: " + path);
                        }
                    }
                    path = item.getThumbnailPath();
                    if (path != null) {
                        result = pictureManager.deleteFile(path);
                        if (!result) {
                            if(E) Log.e(TAG, "File not deleted: " + path);
                        }
                    }
                }
            } catch (RuntimeException e) {
                if(E) Log.e(TAG, "RuntimeException", e);
                if (onErrorListener != null) {
                    onErrorListener.onError(e);
                }
            }
        });
    }
}
