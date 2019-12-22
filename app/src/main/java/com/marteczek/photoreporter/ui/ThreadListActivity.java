package com.marteczek.photoreporter.ui;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.configuration.viewmodelfactory.ViewModelFactory;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class ThreadListActivity extends AppCompatActivity {

    public static final String EXTRA_THREAD_ID = "extra_thread_id";
    public static final String EXTRA_THREAD_NAME = "extra_thread_name";

    private static final int FIND_THREAD_REQUEST_CODE = 1;

    @Inject
    ViewModelFactory viewModelFactory;

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
        ThreadListViewModel viewModel = new ViewModelProvider(this, viewModelFactory).get(ThreadListViewModel.class);
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
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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
