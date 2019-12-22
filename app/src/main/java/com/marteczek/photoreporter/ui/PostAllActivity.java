package com.marteczek.photoreporter.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.Settings;
import com.marteczek.photoreporter.application.configuration.viewmodelfactory.ViewModelFactory;
import com.marteczek.photoreporter.database.entity.Post;
import com.marteczek.photoreporter.database.entity.Report;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.marteczek.photoreporter.application.Settings.Debug.D;
import static com.marteczek.photoreporter.application.Settings.Debug.DONT_PUBLISH_POSTS;

public class PostAllActivity extends AppCompatActivity {

    private static final String TAG = "PostAllActivity";

    public static final String EXTRA_REPORT_ID = "report_id";

    private static final String STOP_WAITING = "stop_waiting";

    private static final String FORM_URL = "https://www.skyscrapercity.com/newreply.php?do=newreply&noquote=1&t=";

    private static final int POST_DELAY = 31;

    private WebView webView;

    private PostAllViewModel viewModel;

    private Handler handler;

    private String thread;

    private LiveData<List<Post>> posts;

    private boolean isEditorReady = false;

    @Inject
    ViewModelFactory viewModelFactory;

    private Runnable waitForEditor = new Runnable() {

        @Override
        public void run() {
            if(D) Log.d(TAG, "Waiting for the vB_editor");
            webView.loadUrl("javascript:(function(){ " +
                    "if (typeof vB_Editor !== 'undefined') {" +
                    "    alert('" + STOP_WAITING + "');" +
                    "}" +
                    "})()");
            handler.postDelayed(this, 2000);
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_all);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Long reportId = getIntent().getLongExtra(EXTRA_REPORT_ID, 0);
        if (reportId == 0) {
            Toast.makeText(this, R.string.no_report_found, Toast.LENGTH_LONG).show();
            finish();
        }
        handler = new Handler(getApplication().getMainLooper());
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(PostAllViewModel.class);
        LiveData<Report> report = viewModel.getReport(reportId);
        report.observe(this, (r) -> {
            if (r != null && r.getThreadId() != null) {
                thread = r.getThreadId();
                String url = FORM_URL + thread;
                if(D) Log.d(TAG, url);
                webView.loadUrl(url);
            }
            if (thread == null) {
                Toast.makeText(this, R.string.thread_not_set, Toast.LENGTH_LONG).show();
                finish();
            }
        });
        posts = viewModel.getPosts(reportId);
        posts.observe(this, (p) -> {
            if (p != null && p.size() == 0) {
                Toast.makeText(this, R.string.no_posts_found, Toast.LENGTH_LONG).show();
                finish();
            }
            fillForm();
        });
        TextView timeTextView = findViewById(R.id.time);
        LiveData<Integer> timer = viewModel.getTimer();
        timer.observe(this, time -> {
            if(D) Log.d(TAG, "Timer, time: " + time);
            timeTextView.setText(String.valueOf(time));
            if (Integer.valueOf(0).equals(time)) {
                nextPost();
            }
        });
        Button button = findViewById(R.id.send);
        button.setEnabled(false);
        button.setOnClickListener((view -> {
            if (thread != null) {
                viewModel.setSending(true);
                webView.loadUrl("javascript:(function(){ " +
                        "alert(document.getElementsByTagName('form')[0].action);" +
                        "})()");
            }
        }));
        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString(Settings.DEFAULT_USER_AGENT);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String strUrl) {
                super.onPageFinished(view, strUrl);
                if(D) Log.d(TAG, "onPageFinished url:  " + strUrl);
                if (strUrl != null && strUrl.contains("showthread.php")) {

//                    nextPost();
                    waitOrFinish();
                } else {
                    webView.loadUrl("javascript:(function(){ " +
                            "alert(document.getElementsByTagName('form')[0].action);" +
                            "})()");
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            if(D) Log.d(TAG, "onJsAlert url: " + url + "message: " + message);
            if (message.contains("login.php?do=login")) {
                Toast.makeText(PostAllActivity.this, R.string.imgur_please_sign_in,
                        Toast.LENGTH_LONG).show();
                viewModel.setSending(false);
            } else {
                button.setEnabled(!viewModel.isSending());
                if (viewModel.isSending()) {
                    if (message.contains("newreply.php?do=postreply")) {
                        handler.post(waitForEditor);
                    } else {
                        if (STOP_WAITING.equals(message)) {
                            handler.removeCallbacks(waitForEditor);
                            isEditorReady = true;
                            fillForm();
                        }
                    }
                }
            }
            result.confirm();
            return true;
        }});

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(waitForEditor);
        if (isFinishing()) {
            viewModel.stopTimer();
        }
        super.onDestroy();
    }

    private void fillForm() {
        List<Post> postList = posts.getValue();
        if (isEditorReady && postList != null) {
            int postNumber = viewModel.getPostNumber();
            if (postNumber <= postList.size()) {
                Post post = postList.get( postNumber - 1);
                String title = "";
                String content = post.getContent();
                content = content
                        .replace("\n", "\\n")
                        .replace("\r", "\\r");
                if(D) Log.d(TAG, "content:\n" + content);
                if (DONT_PUBLISH_POSTS) {
                    webView.loadUrl("javascript:(function(){ " +
                            "document.getElementsByName('title')[0].value = '" + title + "'; " +
                            "document.getElementsByName('message')[0].value = \"" + content + "\"; " +
                            "})()");
                    handler.postDelayed(() ->
                            webView.loadUrl("https://www.skyscrapercity.com/showthread.php?t="
                                    + thread), 4000);
                } else {
                    webView.loadUrl("javascript:(function(){ " +
                            "document.getElementsByName('title')[0].value = '" + title + "'; " +
                            "document.getElementsByName('message')[0].value = \"" + content + "\"; " +
                            "})()");
                    handler.postDelayed(()->
                            webView.loadUrl("javascript:(function(){ " +
                                "document.getElementsByName('message')[0].form.submit(); " + "})()"
                            ), 200);
                }
            }
        }
    }

    private void waitOrFinish() {
        int postNumber = viewModel.getPostNumber() + 1;
        viewModel.setPostNumber(postNumber);
        if (postNumber > posts.getValue().size()) {
            finish();
        } else {
            viewModel.startTimer(POST_DELAY);
        }
    }

    private void nextPost() {
        String url = FORM_URL + thread;
        if(D) Log.d(TAG, "next post url:" + url);
        webView.loadUrl(url);
    }
}
