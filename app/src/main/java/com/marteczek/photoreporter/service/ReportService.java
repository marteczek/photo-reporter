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
import com.marteczek.photoreporter.database.entity.type.ReportStatus;
import com.marteczek.photoreporter.picturemanager.PictureManager;
import com.marteczek.photoreporter.imagetools.ImageUtils;
import com.marteczek.photoreporter.service.baseservice.BaseService;
import com.marteczek.photoreporter.service.data.PictureItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

    public  LiveData<List<String>> findThreadsIds() {
        return reportDao.findThreadsIds();
    }

    public void insertItemsToReport(final Long reportId,
                                    final List<PictureItem> listOfPictures,
                                    final int thumbnailDimension,
                                    final OnFinishedListener onFinishedListener,
                                    final OnErrorListener onErrorListener) {
        List<PictureItem> pictures = Collections.synchronizedList(new ArrayList<>());
        pictures.addAll(listOfPictures);
        executeInTransaction(() -> {
            try {
                Report report = reportDao.findById(reportId);
                if (report != null && ReportStatus.NEW.equals(report.getStatus())) {
                    Long succession = itemDao.findMaxSuccessionByReportId(reportId);
                    succession = succession == null ? 0L : succession + 1;
                    for (PictureItem pictureItem : listOfPictures) {
                        Uri uri = pictureItem.getPictureUri();
                        if (itemDao.findByReportIdAndPictureUri(reportId, uri.toString()).size()
                                == 0) {
                            Long itemId = itemDao.insert(Item.builder()
                                    .reportId(reportId)
                                    .pictureUri(uri.toString())
                                    .thumbnailRequiredWidth(thumbnailDimension)
                                    .thumbnailRequiredHeight(thumbnailDimension)
                                    .status("new")
                                    .build());
                            try {
                                String path = pictureManager.copy(uri, "picture" + itemId);
                                itemDao.updatePicturePathById(itemId, path);
                                itemDao.updatePictureRotationById(itemId, ImageUtils.getImageRotation(path));
                                itemDao.updateSuccessionById(itemId, succession++);
                            } catch (IOException e) {
                                if (E) Log.e(TAG, "IOException", e);
                                itemDao.deleteById(itemId);
                            }
                        }
                    }
                    if (onFinishedListener != null) {
                        Handler handler = new Handler(context.getMainLooper());
                        handler.post(() -> onFinishedListener.onFinished(reportId));
                    }
                } else {
                    throw new IllegalStateException();
                }
            } catch (RuntimeException e) {
                if(E) Log.e(TAG, "RuntimeException", e);
                if (onErrorListener != null) {
                    onErrorListener.onError(e);
                }
            }
        });
    }

    public void insertReportWithItems(final Report report,
                                      final List<PictureItem> listOfPictures,
                                      final int thumbnailDimension,
                                      final OnFinishedListener onFinishedListener,
                                      final OnErrorListener onErrorListener) {
        final List<PictureItem> pictures = Collections.synchronizedList(new ArrayList<>());
        pictures.addAll(listOfPictures);
        executeInTransaction(() -> {
            try {
                final long reportId = reportDao.insert(report);
                long succession = 0L;
                for (PictureItem pictureItem : pictures) {
                    Uri uri = pictureItem.getPictureUri();
                    Long itemId = itemDao.insert(Item.builder()
                            .reportId(reportId)
                            .pictureUri(uri.toString())
                            .thumbnailRequiredWidth(thumbnailDimension)
                            .thumbnailRequiredHeight(thumbnailDimension)
                            .status("new")
                            .build());
                    try {
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
            } catch (final RuntimeException e) {
                if(E) Log.e(TAG, "RuntimeException", e);
                if (onErrorListener != null) {
                    Handler handler = new Handler(context.getMainLooper());
                    handler.post(() -> onErrorListener.onError(e));
                }
            }
        });
    }

    public void updateReportName(final Long id, final String name) {
        ReportDatabase.databaseWriteExecutor.execute(() -> reportDao.updateNameById(id, name));
    }

    public void updateThreadId(final Long id, final String newThreadId) {
        ReportDatabase.databaseWriteExecutor.execute(() ->
                reportDao.updateThreadId(id, newThreadId));
    }

    public void deleteReportWithDependencies(final Long reportId,
                                             final OnErrorListener onErrorListener) {
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
