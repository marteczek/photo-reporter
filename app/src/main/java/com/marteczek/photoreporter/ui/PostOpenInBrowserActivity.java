package com.marteczek.photoreporter.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.Settings;
import com.marteczek.photoreporter.application.configuration.viewmodelfactory.ViewModelFactory;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.marteczek.photoreporter.application.Settings.Debug.D;

public class PostOpenInBrowserActivity extends AppCompatActivity {

    private static final String TAG = "PostOpenInBrowser";

    public static final String EXTRA_REPORT_ID = "extra_report_id";

    public static final String EXTRA_CONTENT = "extra_content";

    private static final String STOP_WAITING = "stop_waiting";

    private static final String FORM_URL = "https://www.skyscrapercity.com/newreply.php?do=newreply&noquote=1&t=";

    private WebView webView;

    private Handler handler;

    private String content;

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
            handler.postDelayed(this, 200);
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_in_browser);
        PostOpenInBrowserViewModel viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(PostOpenInBrowserViewModel.class);
        handler = new Handler(getApplication().getMainLooper());
        Intent intent = getIntent();
        Long reportId = intent.getLongExtra(EXTRA_REPORT_ID, 0);
        content = intent.getStringExtra(EXTRA_CONTENT);
        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString(Settings.DEFAULT_USER_AGENT);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String strUrl) {
                super.onPageFinished(view, strUrl);
                if(D) Log.d(TAG, "onPageFinished url: " + strUrl);
                if (strUrl.contains("login.php?do=login") || strUrl.contains("newreply.php?do=newreply")) {
                    webView.loadUrl("javascript:(function(){ " +
                            "alert(document.getElementsByTagName('form')[0].action);" +
                            "})()");
                } else {
                    if(D) Log.d(TAG, "Page not recognized");
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                if(D) Log.d(TAG, "onJsAlert url: " + url + ", message: " + message);
                if (message.contains("login.php?do=login")) {
                    Toast.makeText(PostOpenInBrowserActivity.this, R.string.imgur_please_sign_in,
                            Toast.LENGTH_LONG).show();
                } else {
                    if (message.contains("newreply.php?do=postreply")) {
                        handler.postDelayed(waitForEditor, 1000);
                    } else {
                        if (STOP_WAITING.equals(message)) {
                            handler.removeCallbacks(waitForEditor);
                            fillForm();
                        }
                    }
                }
                result.confirm();
                return true;
            }
        });
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        }
        viewModel.getReport(reportId).observe(this, report -> {
            String threadId = report.getThreadId();
            if (threadId != null && savedInstanceState == null) {
                webView.loadUrl(FORM_URL + threadId);
            } else {
                Toast.makeText(this, R.string.thread_not_set, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(waitForEditor);
        super.onDestroy();
    }

    private void fillForm() {
        String title = "";
        String content = this.content
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        if(D) Log.d(TAG, "content:\n" + content);
        webView.loadUrl("javascript:(function(){ " +
                "document.getElementsByName('title')[0].value = '" + title + "'; " +
                "document.getElementsByName('message')[0].value = \"" + content + "\"; " +
                "})()");
    }
}

