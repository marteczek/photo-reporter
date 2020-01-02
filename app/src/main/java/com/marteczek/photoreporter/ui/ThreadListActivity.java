package com.marteczek.photoreporter.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.configuration.viewmodelfactory.ViewModelFactory;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.marteczek.photoreporter.application.Settings.Debug.D;
import static java.lang.Math.round;

public class ThreadListActivity extends AppCompatActivity {
    private static final String TAG = "ThreadListActivity";

    public static final String EXTRA_THREAD_ID = "extra_thread_id";
    public static final String EXTRA_THREAD_NAME = "extra_thread_name";

    private static final int FIND_THREAD_REQUEST_CODE = 1;

    @Inject
    ViewModelFactory viewModelFactory;
    private ThreadListViewModel viewModel;

    public interface ItemTouchHelperAdapter {

        int getMovementFlags(int position);

        String onItemDismiss(int position);
    }

    public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

        private final ItemTouchHelperAdapter adapter;

        private ItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
            this.adapter= adapter;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            return adapter.getMovementFlags(viewHolder.getAdapterPosition());
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            String threadId = adapter.onItemDismiss(viewHolder.getAdapterPosition());
            if (threadId != null) {
                viewModel.removeThread(threadId);
            }
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
                Drawable drawable = AppCompatResources.getDrawable(ThreadListActivity.this, R.drawable.ic_delete_white_36dp);
                if (drawable != null) {
                    int margin = round(16 * (getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
                    int dim = round(24 * (getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
                    int mid = (itemView.getTop() + itemView.getBottom()) / 2;
                    drawable.setBounds(itemView.getLeft() + margin, mid - dim / 2,
                            itemView.getLeft() + margin + dim, mid + dim / 2);
                    c.clipRect(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom());
                    drawable.draw(c);
                }
                float alpha = 1f;
                if (dX < viewHolder.itemView.getWidth()) {
                    alpha = 1f - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
                }
                viewHolder.itemView.setAlpha(alpha);
                if(D) Log.d(TAG, "."+ actionState +"." + isCurrentlyActive + "." + dX + ".");
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread_list);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(ThreadListActivity.this, FindThreadActivity.class);
            startActivityForResult(intent, FIND_THREAD_REQUEST_CODE);
        });
        viewModel = new ViewModelProvider(this, viewModelFactory).get(ThreadListViewModel.class);
        ThreadListAdapter adapter = new ThreadListAdapter(this,
                thread -> {
                    Intent data = new Intent();
                    data.putExtra(EXTRA_THREAD_ID, thread.getThreadId());
                    data.putExtra(EXTRA_THREAD_NAME, thread.getName());
                    setResult(RESULT_OK, data);
                    finish();
                }
        );
        viewModel.getThreads().observe(this, adapter::setThreads);
        viewModel.getThreadsIdsInReports().observe(this, adapter::setThreadsIdsInReports);
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper.Callback callback = new ThreadListActivity.ItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FIND_THREAD_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_THREAD_ID,
                    data.getStringExtra(FindThreadActivity.EXTRA_THREAD_ID));
            intent.putExtra(EXTRA_THREAD_NAME,
                    data.getStringExtra(FindThreadActivity.EXTRA_THREAD_NAME));
            setResult(RESULT_OK, data);
            finish();
        }
    }
}
