package com.marteczek.photoreporter.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.configuration.viewmodelfactory.ViewModelFactory;
import com.marteczek.photoreporter.database.entity.Post;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class PostActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_ID = "report_id";

    private PostViewModel viewModel;

    private Long reportId;

    private EditText postEditText;

    private Spinner postsSpinner;

    Long currentPostId;

    LiveData<List<Post>> postsLiveData;

    List<Post> posts;

    ArrayAdapter<String> postsAdapter;

    @Inject
    ViewModelFactory viewModelFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        postEditText = findViewById(R.id.post);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(PostViewModel.class);
        Intent intent = getIntent();
        reportId = intent.getLongExtra(EXTRA_REPORT_ID, 0);
        viewModel.findPostsByReportId(reportId).observe(this, posts -> {
            if (this.posts == null) {
                this.posts = posts;
                if (posts.size() == 0) {
                    Toast.makeText(this, R.string.no_report_found, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    for (Post post : posts) {
                        postsAdapter.add("Post " + post.getNumber());
                    }
                    postsSpinner.setEnabled(true);
                }
            }
        });
        postsLiveData = viewModel.findPostsByReportId(reportId);
        postsSpinner = findViewById(R.id.posts);
        postsSpinner.setEnabled(false);
        postsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>());
        postsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        postsSpinner.setAdapter(postsAdapter);
        postsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (posts != null) {
                    saveCurrentPost();
                    Post post = posts.get(position);
                    currentPostId = post.getId();
                    postEditText.setText(post.getContent());
                    postEditText.setEnabled(true);
                } else {
                    postEditText.setText("");
                    postEditText.setEnabled(false);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                postEditText.setText("");
                postEditText.setEnabled(false);
            }
        });
    }

    @Override
    protected void onStop() {
        saveCurrentPost();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        switch (id) {
            case R.id.action_copy:
                ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("text", postEditText.getText());
                manager.setPrimaryClip(clipData);
                return true;
            case R.id.action_open_in_browser:
                if(reportId != null) {
                    intent = new Intent(this, PostOpenInBrowserActivity.class);
                    intent.putExtra(PostOpenInBrowserActivity.EXTRA_REPORT_ID, reportId);
                    intent.putExtra(PostOpenInBrowserActivity.EXTRA_CONTENT,
                            postEditText.getText().toString());
                    startActivity(intent);
                }
                return true;
            case R.id.action_post_all:
                intent = new Intent(this, PostAllActivity.class);
                intent.putExtra(PostAllActivity.EXTRA_REPORT_ID, reportId);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveCurrentPost() {
        if (currentPostId != null) {
            viewModel.savePost(currentPostId, postEditText.getText().toString());
        }
    }
}
