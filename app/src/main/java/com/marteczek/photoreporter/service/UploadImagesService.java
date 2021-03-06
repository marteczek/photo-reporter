package com.marteczek.photoreporter.service;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.marteczek.photoreporter.database.dao.ItemDao;
import com.marteczek.photoreporter.database.dao.ReportDao;
import com.marteczek.photoreporter.database.entity.Item;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.database.entity.type.ElementStatus;
import com.marteczek.photoreporter.database.entity.type.ReportStatus;
import com.marteczek.photoreporter.imagehostclient.BaseResponse;
import com.marteczek.photoreporter.imagehostclient.imgur.data.PictureMetadata;
import com.marteczek.photoreporter.picturemanager.PictureManager;
import com.marteczek.photoreporter.imagehostclient.ImageHostClient;
import com.marteczek.photoreporter.service.misc.PictureFormat;

import java.text.SimpleDateFormat;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

import static com.marteczek.photoreporter.application.Settings.Debug.D;
import static com.marteczek.photoreporter.application.Settings.Debug.E;

public class UploadImagesService {

    private static final String TAG = "UploadImagesService";

    private final PictureManager pictureManager;

    private final ReportDao reportDao;

    private final ItemDao itemDao;

    public interface UploadManager {
        boolean isStopped();
    }

    public interface ProgressChangedListener {
        void onProgressChanged(int max, int current);
    }

    @Builder
    @Getter
    public static class Response {
        boolean succeeded;
        BaseResponse clientResponse;
    }

    public UploadImagesService(PictureManager pictureManager, ReportDao reportDao, ItemDao itemDao) {
        this.pictureManager = pictureManager;
        this.reportDao = reportDao;
        this.itemDao = itemDao;
    }

    public Response uploadImages(@NonNull Long reportId,
                                 @NonNull ImageHostClient imageHostClient,
                                 @NonNull PictureFormat pictureFormat,
                                 @NonNull UploadManager uploadManager,
                                 @Nullable ProgressChangedListener progressChangedListener) {
        try {
            Report report = reportDao.findById(reportId);
            if (report == null) {
                if(D) Log.d(TAG, "There is no report with given id");
                return Response.builder().succeeded(true).build();
            }
            reportDao.updateStatusById(reportId, ReportStatus.PENDING);
            List<Item> items = itemDao.findByReportIdAndStatusesOrderBySuccession(reportId,
                    new String[]{ElementStatus.NEW, ElementStatus.SENDING});
            if (progressChangedListener != null) {
                progressChangedListener.onProgressChanged(items.size(), 0);
            }
            BaseResponse response;
            response = imageHostClient.connect(items.size());
            if (!response.isContinuable()) {
                reportDao.updateStatusById(reportId, ReportStatus.PICTURE_SENDING_FAILURE);
                itemDao.updateStatusByReportId(reportId, ElementStatus.NEW);
                return Response.builder().succeeded(false).clientResponse(response).build();
            }
            if (uploadManager.isStopped()) {
                reportDao.updateStatusById(reportId, ReportStatus.PICTURE_SENDING_CANCELLED);
                itemDao.updateStatusByReportId(reportId, ElementStatus.NEW);
                return Response.builder().succeeded(false).clientResponse(response).build();
            }
            String albumMetadata = report.getHostMetadata();
            if (albumMetadata == null) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                String title = simpleDateFormat.format(report.getDate());
                response = imageHostClient.createAlbum(title, report.getName());
                if (response.isSuccess()) {
                    albumMetadata = response.getAlbumMetadata();
                    reportDao.updateHostMetadataById(reportId, albumMetadata);
                }
                if (!response.isContinuable()) {
                    reportDao.updateStatusById(reportId, ReportStatus.PICTURE_SENDING_FAILURE);
                    itemDao.updateStatusByReportId(reportId, ElementStatus.NEW);
                    return Response.builder().succeeded(false).clientResponse(response).build();
                }
            }
            if (uploadManager.isStopped()) {
                reportDao.updateStatusById(reportId, ReportStatus.PICTURE_SENDING_CANCELLED);
                itemDao.updateStatusByReportId(reportId, ElementStatus.NEW);
                return Response.builder().succeeded(false).clientResponse(response).build();
            }
            int pictureNumber = 1;
            for (Item item : items) {
                Long itemId = item.getId();
                String pictureName ="picture-" + item.getSuccession() + ".jpg";
                Bitmap bitmap = pictureManager.preparePictureForUpload(item.getPicturePath(),
                        item.getPictureRotation(), pictureFormat.getGreaterDimension());
                String path = pictureManager.savePicture(bitmap, pictureName);
                PictureMetadata pictureMetadata;
                for (int i = 0; i < 3 ; i++) {
                    response = imageHostClient.uploadImage(path, pictureName, "desc.");
                    if (response.isSuccess() || !response.isRetryable()) {
                        break;
                    } else {
                        if (uploadManager.isStopped()) {
                            reportDao.updateStatusById(reportId, ReportStatus.PICTURE_SENDING_CANCELLED);
                            itemDao.updateStatusByReportId(reportId, ElementStatus.NEW);
                            return Response.builder().succeeded(false).clientResponse(response).build();
                        }
                        if(D) Log.d(TAG, "Retrying upload file: " + item.getSuccession());
                    }
                }
                if (response.isSuccess()) {
                    if (progressChangedListener != null) {
                        progressChangedListener.onProgressChanged(items.size(), pictureNumber);
                    }
                    pictureNumber++;
                    pictureMetadata = response.getPictureMetadata();
                    itemDao.updatePictureUrlById(itemId, pictureMetadata.getLink());
                    itemDao.updateMetadataById(itemId, pictureMetadata.getMetadata());
                    itemDao.updateStatusById(itemId, ElementStatus.SENT);
                }
                if (uploadManager.isStopped()) {
                    reportDao.updateStatusById(reportId, ReportStatus.PICTURE_SENDING_CANCELLED);
                    itemDao.updateStatusByReportId(reportId, ElementStatus.NEW);
                    return Response.builder().succeeded(false).clientResponse(response).build();
                }
                if (!response.isContinuable()) {
                    reportDao.updateStatusById(reportId, ReportStatus.PICTURE_SENDING_FAILURE);
                    itemDao.updateStatusByReportId(reportId, ElementStatus.NEW);
                    return Response.builder().succeeded(false).clientResponse(response).build();
                }
            }
            reportDao.updateStatusById(reportId, ReportStatus.SENT);
            imageHostClient.disconnect();
            return Response.builder().succeeded(true).build();
        } catch (RuntimeException e) {
            if(E) Log.e(TAG, "RuntimeException", e);
            return Response.builder().succeeded(false).build();
        }
    }
}