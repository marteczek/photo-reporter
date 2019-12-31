package com.marteczek.photoreporter.ui;

import android.app.Application;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.Settings;
import com.marteczek.photoreporter.application.background.ThumbnailWorker;
import com.marteczek.photoreporter.application.background.UploadWorker;
import com.marteczek.photoreporter.database.entity.ForumThread;
import com.marteczek.photoreporter.database.entity.Item;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.service.ItemService;
import com.marteczek.photoreporter.service.PostService;
import com.marteczek.photoreporter.service.ReportService;
import com.marteczek.photoreporter.service.ThreadService;
import com.marteczek.photoreporter.service.data.PictureItem;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReportViewModel extends AndroidViewModel {

    private ReportService reportService;

    private PostService postService;

    private ItemService itemService;

    private ThreadService threadService;

    public ReportViewModel(Application application,
                           ReportService reportService, PostService postService,
                           ItemService itemService, ThreadService threadService) {
        super(application);
        this.reportService = reportService;
        this.postService = postService;
        this.itemService = itemService;
        this.threadService = threadService;
    }

    LiveData<List<Item>> findItemByReportId(Long id) {
        return itemService.findItemByReportId(id);
    }

    LiveData<Report> findReportById(Long id) {
        return reportService.findReportByIdAsync(id);
    }

    void updateReportName(Long id, String name){
        reportService.updateReportName(id, name);
    }

    void updateThreadId(Long id, String newThreadId) {
        reportService.updateThreadId(id, newThreadId);
    }

     LiveData<ForumThread> findThreadByThreadId(String threadId) {
        return threadService.findThreadByThreadId(threadId);
    }

    LiveData<WorkInfo> uploadPictures(Long reportId) {
        WorkManager workManager = WorkManager.getInstance(getApplication());
        Data data = new Data.Builder()
                .putLong(UploadWorker.DATA_REPORT_ID, reportId)
                .build();
        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(UploadWorker.class)
                .setInputData(data)
                .build();
        workManager.enqueueUniqueWork(UploadWorker.NAME,
                ExistingWorkPolicy.KEEP, uploadWorkRequest);
        return workManager.getWorkInfoByIdLiveData(uploadWorkRequest.getId());
    }

    void createPost(Long reportId) {
        String footer = "";
        String signature = Settings.getSignature(getApplication());
        if (!TextUtils.isEmpty(signature)) {
            footer = signature + "\n";
        }
        if(Settings.shouldAttachAppFooter(getApplication())) {
            footer += getApplication().getString(R.string.application_footer);
        }
        postService.generatePosts(reportId, Settings.getPicturesPerPost(getApplication()), footer,
                e -> Toast.makeText(getApplication(), R.string.database_error, Toast.LENGTH_LONG).show());
    }

    void updateItems(Map<Long, String> headerChanges, Set<Long> removedItems,
                            List<Long> order) {
        itemService.updateItems(headerChanges, removedItems, order,
                e -> Toast.makeText(getApplication(), R.string.database_error, Toast.LENGTH_LONG).show());
    }

    void addItems(Long reportId, List<PictureItem> listOfPictures) {
        reportService.insertItemsToReport(reportId, listOfPictures,
                Settings.getThumbnailDimension(getApplication()),
                repId -> {
                    Data data = new Data.Builder()
                            .putLong(ThumbnailWorker.DATA_REPORT_ID, repId)
                            .build();
                    OneTimeWorkRequest thumbnailWorkRequest =
                            new OneTimeWorkRequest.Builder(ThumbnailWorker.class)
                                    .setInputData(data)
                                    .build();
                    WorkManager.getInstance(getApplication()).enqueueUniqueWork(ThumbnailWorker.NAME,
                            ExistingWorkPolicy.REPLACE, thumbnailWorkRequest);},
                e -> Toast.makeText(getApplication(), R.string.database_error, Toast.LENGTH_LONG).show());
    }
}
