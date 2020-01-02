package com.marteczek.photoreporter.ui;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.Settings;
import com.marteczek.photoreporter.application.data.ImgurUserData;
import com.marteczek.photoreporter.application.configuration.viewmodelfactory.ViewModelFactory;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.service.data.PictureItem;
import com.marteczek.photoreporter.ui.misc.ItemTouchHelperAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.marteczek.photoreporter.application.Settings.Debug.D;
import static java.lang.Math.round;

public class ReportListActivity extends AppCompatActivity {
    private static final String TAG = "ReportListActivity";

    private static final int CHOOSE_PICTURES_REQUEST_CODE = 1;
    private static final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 2;

    private static final String NEW_REPORT_NAME = "new_report_name";

    private ReportListViewModel viewModel;

    private String newReportName;

    @Inject
    ViewModelFactory viewModelFactory;

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
            return makeMovementFlags(0, ItemTouchHelper.END);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
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
                Drawable drawable = AppCompatResources.getDrawable(ReportListActivity.this, R.drawable.ic_delete_white_36dp);
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
        if(savedInstanceState != null) {
            newReportName = savedInstanceState.getString(NEW_REPORT_NAME);
        }
        setContentView(R.layout.activity_report_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> getNameAndStartPicturesActivity());
        viewModel = new ViewModelProvider(this, viewModelFactory).get(ReportListViewModel.class);
        ReportListAdapter adapter = new ReportListAdapter(this,
            report -> {
                Intent intent = new Intent(ReportListActivity.this,
                        ReportActivity.class);
                intent.putExtra(ReportActivity.EXTRA_REPORT_ID, report.getId());
                startActivity(intent);
            },
            report -> viewModel.removeReport(report)

        );
        viewModel.getReports().observe(this, adapter::setReports);
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper.Callback callback = new ReportListActivity.ItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        ImgurUserData userData = Settings.getImgurUserData(this);
        if (userData == null) {
            Toast.makeText(this,R.string.please_log_in_to_upload, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_report_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        switch (id) {
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_manage_accounts:
                intent = new Intent(this, AccountManagerActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_PICTURES_REQUEST_CODE && resultCode == RESULT_OK && data != null){
            List<PictureItem> listOfPictures = new ArrayList<>();
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
                    Date lastModified = null;
                    if (documentFile != null) {
                        lastModified = new Date(documentFile.lastModified());
                    }
                    PictureItem pictureItem = PictureItem.builder().pictureUri(uri)
                            .lastModified(lastModified).build();
                    listOfPictures.add(pictureItem);
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
                Date lastModified = null;
                if (documentFile != null) {
                    lastModified = new Date(documentFile.lastModified());
                }
                PictureItem pictureItem = PictureItem.builder().pictureUri(uri)
                        .lastModified(lastModified).build();
                listOfPictures.add(pictureItem);
            }
            viewModel.insertReportWithItems(
                    Report.builder().name(newReportName).date(new Date()).status("new").build(),
                    listOfPictures);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_EXTERNAL_STORAGE_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PERMISSION_GRANTED) {
            choosePictures();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(NEW_REPORT_NAME, newReportName);
    }

    private void getNameAndStartPicturesActivity() {
        final EditText nameEditText = new EditText(this);
        nameEditText.setLayoutParams( new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.report_title)
            .setMessage(R.string.name)
            .setView(nameEditText)
            .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                String name = nameEditText.getText().toString();
                if (!TextUtils.isEmpty(name)) {
                    newReportName = name;
                    if (ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
                            == PERMISSION_GRANTED) {
                        choosePictures();
                    } else
                        ActivityCompat.requestPermissions(this,
                                new String[]{READ_EXTERNAL_STORAGE},
                                READ_EXTERNAL_STORAGE_REQUEST_CODE);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.report_name_cant_be_empty,
                            Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    private void choosePictures() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        startActivityForResult(Intent.createChooser(intent,
                getResources().getString(R.string.select_pictures)), CHOOSE_PICTURES_REQUEST_CODE);
    }
}
