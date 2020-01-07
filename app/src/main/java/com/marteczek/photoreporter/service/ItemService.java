package com.marteczek.photoreporter.service;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.marteczek.photoreporter.application.MainThreadRunner;
import com.marteczek.photoreporter.database.ReportDatabaseHelper;
import com.marteczek.photoreporter.database.dao.ItemDao;
import com.marteczek.photoreporter.database.entity.Item;
import com.marteczek.photoreporter.picturemanager.PictureManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.marteczek.photoreporter.application.Settings.Debug.E;

public class ItemService {

    private static final String TAG = "BaseService";

    private final ReportDatabaseHelper dbHelper;

    private final PictureManager pictureManager;

    private final ItemDao itemDao;

    private MainThreadRunner mainThreadRunner;

    public ItemService(ItemDao itemDao, PictureManager pictureManager,
                       ReportDatabaseHelper reportDatabaseHelper,
                       MainThreadRunner mainThreadRunner) {
        this.dbHelper = reportDatabaseHelper;
        this.itemDao = itemDao;
        this.pictureManager = pictureManager;
        this.mainThreadRunner = mainThreadRunner;
    }

    public LiveData<List<Item>> findItemByReportId(Long id) {
        return itemDao.findByReportIdOrderBySuccessionAsync(id);
    }

    public void createThumbnails(Long reportId, OnErrorListener onErrorListener) {
        try {
            List<Item> items = itemDao.findByReportIdAndThumbnailPathIsNull(reportId);
            for (Item item : items) {
                String thumbnailPath = pictureManager.generateThumbnail(
                        item.getPicturePath(),
                        "thumbnail" + item.getId(),
                        item.getThumbnailRequiredWidth(),
                        item.getThumbnailRequiredHeight());
                itemDao.updateThumbnailPathById(item.getId(), thumbnailPath);
            }
        } catch (RuntimeException e) {
            if(E) Log.e(TAG, "RuntimeException", e);
            if (onErrorListener != null) {
                onErrorListener.onError(e);
            }
        }
    }

    public void updateItems(Map<Long, String> headerChanges, Set<Long> removedItems,
                            List<Long> order, final OnErrorListener onErrorListener) {
        @SuppressLint("UseSparseArrays")
        final Map<Long, String> copyOfHeaderChanges = new ConcurrentHashMap<>(headerChanges);
        final Set<Long> copyOfRemovedItems = Collections.newSetFromMap(new ConcurrentHashMap<>());
        copyOfRemovedItems.addAll(removedItems);
        final List<Long> copyOfOrder = order != null ? Collections.synchronizedList(new ArrayList<>()) : null;
        if (copyOfOrder != null) {
            copyOfOrder.addAll(order);
        }
        dbHelper.executeInTransaction(() -> {
            try {
                for (Long id : copyOfHeaderChanges.keySet()) {
                    itemDao.updateHeaderById(id, copyOfHeaderChanges.get(id));
                }
                for (Long id : copyOfRemovedItems) {
                    itemDao.deleteById(id);
                }
                if (copyOfOrder != null) {
                    Long index = 1L;
                    for (Long id : copyOfOrder) {
                        itemDao.updateSuccessionById(id, index);
                        index++;
                    }
                }
            } catch (RuntimeException e) {
                if(E) Log.e(TAG, "RuntimeException", e);
                if (onErrorListener != null) {
                    mainThreadRunner.run(() -> onErrorListener.onError(e));
                }
        }});
    }
}
