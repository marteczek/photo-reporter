package com.marteczek.photoreporter.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.background.UploadWorker;
import com.marteczek.photoreporter.application.configuration.viewmodelfactory.ViewModelFactory;
import com.marteczek.photoreporter.database.entity.Item;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.database.entity.type.ReportStatus;
import com.marteczek.photoreporter.ui.misc.ItemTouchHelperAdapter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.marteczek.photoreporter.application.Settings.Debug.D;
import static com.marteczek.photoreporter.application.Settings.Debug.E;
import static java.lang.Math.round;

public class ReportActivity extends AppCompatActivity {

    private static final String TAG = "ReportActivity";

    public static final String EXTRA_REPORT_ID = "extra_report_id";

    private static final int SELECT_THREAD_REQUEST_CODE = 1;

    private ReportViewModel viewModel;

    private Menu menu;

    private ItemListAdapter adapter;

    private String newThreadId;

    private long reportId;

    private Report report;

    private WorkInfo.State uploadWorkerState;

    @Inject
    ViewModelFactory viewModelFactory;

    public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

        private final ItemTouchHelperAdapter adapter;

        private ItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
             this.adapter= adapter;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return report != null && ReportStatus.NEW.equals(report.getStatus());
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder) {
            if (viewHolder.getAdapterPosition() == 0) {
                return makeMovementFlags(0, 0);
            } else {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }
        }

        @Override
        public boolean canDropOver(@NonNull RecyclerView recyclerView,
                                   @NonNull RecyclerView.ViewHolder current,
                                   @NonNull RecyclerView.ViewHolder target) {
            return target.getAdapterPosition() != 0;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                              RecyclerView.ViewHolder target) {
            adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            adapter.onItemDismiss(viewHolder.getAdapterPosition());
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            View itemView = viewHolder.itemView;
            if (dX > 0) {
                Paint p = new Paint();
                p.setARGB(0xff, 0xff, 0, 0);
                c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX,
                        (float) itemView.getBottom(), p);
                Drawable drawable = AppCompatResources.getDrawable(ReportActivity.this,
                        R.drawable.ic_delete_white_36dp);
                if (drawable != null) {
                    int margin = round(16 * (getResources().getDisplayMetrics().xdpi
                            / DisplayMetrics.DENSITY_DEFAULT));
                    int dim = round(24 * (getResources().getDisplayMetrics().xdpi
                            / DisplayMetrics.DENSITY_DEFAULT));
                    int mid = (itemView.getTop() + itemView.getBottom()) / 2;
                    drawable.setBounds(itemView.getLeft() + margin, mid - dim / 2,
                            itemView.getLeft() + margin + dim, mid + dim / 2);
                    c.clipRect(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom());
                    drawable.draw(c);
                }
                final float alpha = 1f - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
                viewHolder.itemView.setAlpha(alpha);
                viewHolder.itemView.setTranslationX(dX);

            } else {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        viewModel = new ViewModelProvider(this, viewModelFactory).get(ReportViewModel.class);
        Intent intent = getIntent();
        reportId = intent.getLongExtra(EXTRA_REPORT_ID, 0);
        adapter = new ItemListAdapter(this,
                item -> {

                    //TODO: for example, show full screen picture + rotation
                    Intent picturePreviewIntent = new Intent(this,
                            PicturePreviewActivity.class);
                    picturePreviewIntent.putExtra(PicturePreviewActivity.EXTRA_PICTURE_PATH,
                            item.getPicturePath());
                    picturePreviewIntent.putExtra(PicturePreviewActivity.EXTRA_ROTATION,
                            item.getPictureRotation());
                    startActivity(picturePreviewIntent);
                },
                ()->{
                    final Intent intentSelectThread = new Intent(ReportActivity.this,
                            ThreadListActivity.class);
                    startActivityForResult(intentSelectThread, SELECT_THREAD_REQUEST_CODE);
                });
        viewModel.findReportById(reportId).observe(this, report -> {
            this.report = report;
            if (report != null) {
                adapter.setReport(report);
                if (newThreadId != null) {
                    updateThreadName(newThreadId);
                } else {
                    updateThreadName(report.getThreadId());
                }
            } else {
                Toast.makeText(this, R.string.no_report_found, Toast.LENGTH_LONG).show();
                finish();
            }
        });
        viewModel.findItemByReportId(reportId).observe(this,
                items -> adapter.setItems(items));
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
        WorkManager workManager = WorkManager.getInstance(this);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        try {
            List<WorkInfo> infoList = workManager.getWorkInfosForUniqueWork(UploadWorker.NAME).get();
            if (infoList.size() == 1) {
                WorkInfo info = infoList.get(0);
                if(D) Log.d(TAG, "State " + info.getState());
                workManager.getWorkInfoByIdLiveData(info.getId())
                        .observe(this, workInfo -> {
                            if (workInfo != null) {
                                updateProgress(workInfo);
                            }
                        });
            }
        } catch (ExecutionException e) {
            if(E) Log.e(TAG, "ExecutionException", e);
        } catch (InterruptedException e) {
            if(E) Log.e(TAG, "InterruptedException", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_report, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        this.menu = menu;
        configureMenu();
        return super.onPrepareOptionsMenu(menu);
    }

    private void configureMenu() {
        boolean uploadPicturesEnabled = report != null
                && (ReportStatus.NEW.equals(report.getStatus())
                    || ReportStatus.PICTURE_SENDING_FAILURE.equals(report.getStatus())
                    || ReportStatus.PICTURE_SENDING_CANCELLED.equals(report.getStatus())
                )
                && (uploadWorkerState == null || uploadWorkerState.isFinished());
        boolean createPostEnabled = report != null
                && Arrays.asList(ReportStatus.SENT, ReportStatus.POST_CREATED, ReportStatus.PUBLISHED)
                .contains(report.getStatus());
        boolean openPostEnabled = report != null
                && Arrays.asList(ReportStatus.POST_CREATED, ReportStatus.PUBLISHED)
                .contains(report.getStatus());
        if (menu != null) {
            menu.findItem(R.id.action_upload_pictures).setEnabled(uploadPicturesEnabled);
            menu.findItem(R.id.action_create_post).setEnabled(createPostEnabled);
            menu.findItem(R.id.action_open_post).setEnabled(openPostEnabled);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_upload_pictures:
                saveChanges();
                viewModel.uploadPictures(reportId)
                        .observe(this, workInfo -> {
                            if (workInfo != null) {
                                updateProgress(workInfo);
                            }
                        });
                return true;
            case R.id.action_create_post:
                saveChanges();
                viewModel.createPost(reportId);
                return true;
            case R.id.action_open_post:
                saveChanges();
                Intent intent = new Intent(this, PostActivity.class);
                intent.putExtra(PostActivity.EXTRA_REPORT_ID, reportId);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateProgress(WorkInfo workInfo) {
        ProgressBar progressBar = findViewById(R.id.progressBar);
        Data progress = workInfo.getProgress();
        int current = progress.getInt(UploadWorker.DATA_PROGRESS, 0);
        int max = progress.getInt(UploadWorker.DATA_MAX_PROGRESS, 0);
        progressBar.setMax(max);
        progressBar.setProgress(current);
        if (WorkInfo.State.RUNNING.equals(workInfo.getState())) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            invalidateOptionsMenu();
            progressBar.setVisibility(View.GONE);
        }
        uploadWorkerState = workInfo.getState();
        if(D) Log.d(TAG, "Work info: " + uploadWorkerState + ", progress" + current + "/" + max);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_THREAD_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            newThreadId = data.getExtras().getString(ThreadListActivity.EXTRA_THREAD_ID);
            String threadName = data.getExtras().getString(ThreadListActivity.EXTRA_THREAD_NAME);
            if (!TextUtils.isEmpty((threadName))) {
                adapter.setThreadName(threadName);
            } else {
                adapter.setThreadName(newThreadId);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveChanges();
    }
    private void saveChanges() {
        if (adapter.getNewReportName() != null) {
            viewModel.updateReportName(reportId, adapter.getNewReportName());
            adapter.setNewReportName(null);
        }
        if (newThreadId != null) {
            viewModel.updateThreadId(reportId, newThreadId);
            newThreadId = null;
        }
        Map<Long, String> headerChanges = adapter.getItemChanges();
        Set<Long> removedItems = adapter.getRemovedItems();
        if (adapter.isReordered()) {
            List<Item> items = adapter.getItems();
            List<Long> order = new LinkedList<>();
            for(Item item : items) {
                order.add(item.getId());
            }
            viewModel.updateItems(headerChanges, removedItems, order);
        } else {
            viewModel.updateItems(headerChanges, removedItems, null);
        }
        adapter.setReordered(false);
        headerChanges.clear();
        removedItems.clear();
    }

    private void updateThreadName(String threadId) {
        if(threadId != null) {
            viewModel.findThreadByThreadId(threadId).observe(this, thread -> {
                if (!TextUtils.isEmpty(thread.getName())) {
                    adapter.setThreadName(thread.getName());
                } else {
                    adapter.setThreadName(threadId);
                }
            });
        } else {
            adapter.setThreadName("");
        }
    }
}
