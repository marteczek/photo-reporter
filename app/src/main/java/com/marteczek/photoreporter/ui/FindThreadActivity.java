package com.marteczek.photoreporter.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.Settings;
import com.marteczek.photoreporter.database.entity.ForumThread;
import com.marteczek.photoreporter.service.ThreadService;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.marteczek.photoreporter.application.Settings.Debug.D;

public class FindThreadActivity extends AppCompatActivity {
    private static final String TAG = "FindThreadActivity";

    public static final String EXTRA_THREAD_ID = "extra_thread_id";
    public static final String EXTRA_THREAD_NAME = "extra_thread_name";

    protected static final String TOPICS_REF_PATTERN = "^/topics/(\\d+).*$";

    protected static final String TOPICS_QUERY_PATTERN = "(?:^|&)t=(\\d+)(?:$|&)";

    private static final String URL = "https://www.skyscrapercity.com/";

    private WebView webView;

    private String threadId;

    private String threadName;

    private Button chooseThreadButton;

    @Inject
    ThreadService threadService;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        String url = URL;
        if (Settings.Build.TagRestriction != null) {
            try {
                url += "tags.php?tag=" + URLEncoder.encode(Settings.Build.TagRestriction, "UTF-8") ;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        setContentView(R.layout.activity_select_thread);
        final TextView threadView = findViewById(R.id.thread_name);
        threadView.setOnClickListener((view -> threadView.setText(
                Html.fromHtml(webView.getTitle()).toString())));
        webView = findViewById(R.id.browser);
        chooseThreadButton = findViewById(R.id.choose_thread);
        chooseThreadButton.setOnClickListener((view -> {
            threadName = Html.fromHtml(webView.getTitle()).toString();
            ForumThread thread = ForumThread.builder().threadId(threadId).name(threadName)
                    .lastUsage(new Date()).build();
            threadService.saveThread(thread, e -> Toast.makeText(this,
                    R.string.database_error, Toast.LENGTH_LONG).show());
            Intent data = new Intent();
            data.putExtra(EXTRA_THREAD_ID, threadId);
            data.putExtra(EXTRA_THREAD_NAME, threadName);
            setResult(RESULT_OK, data);
            finish();
        }));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString(Settings.DEFAULT_USER_AGENT);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String strUrl) {
                super.onPageFinished(view, strUrl);
                if(D) Log.d(TAG , "onPageFinished, url: " + strUrl);
                try {
                    URL url = new URL(strUrl);
                    String ref = url.getRef();
                    boolean isThread = false;
                    Matcher matcher = null;
                    if(ref != null) {
                        if(D) Log.d(TAG , "ref: " + ref);
                        matcher = Pattern.compile(TOPICS_REF_PATTERN).matcher(ref);
                        isThread = matcher.matches();
                    } else {
                        String path = url.getPath();
                        String query = url.getQuery();
                        if(D) Log.d(TAG ,  "path: " + path);
                        if(D) Log.d(TAG ,  "query: " + query);
                        if ("/showthread.php".equals(path) && query != null) {
                            matcher = Pattern.compile(TOPICS_QUERY_PATTERN).matcher(query);
                            isThread = matcher.matches();
                        }
                    }
                    if (isThread) {
                        threadId = matcher.group(1);
                        threadName = Html.fromHtml(view.getTitle()).toString();
                        if(D) Log.d(TAG , "id: " + threadId);
                        if(D) Log.d(TAG, "name: " + threadName);
                        chooseThreadButton.setEnabled(true);
                        threadView.setText(threadName);
                        return;
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                threadId = null;
                threadName = null;
                threadView.setText("");
                chooseThreadButton.setEnabled(false);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                threadName = Html.fromHtml(title).toString();
                if(D) Log.d(TAG, "WCC name: " + threadName);
            }
        });
        if (savedInstanceState == null) {
            webView.loadUrl(url);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Settings.Build.TagRestriction == null){
            getMenuInflater().inflate(R.menu.menu_find_thread, menu);
            return true;
        } else {
            return super.onCreateOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_find_by_tag) {
            searchByTag();
        }
        return super.onOptionsItemSelected(item);
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
            chooseThreadButton.setEnabled(false);
        } else {
            super.onBackPressed();
        }
    }

    private void searchByTag() {
        final EditText nameEditText = new EditText(this);
        nameEditText.setLayoutParams( new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.search)
                .setMessage(R.string.tag)
                .setView(nameEditText)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    String name = nameEditText.getText().toString();
                    if (!TextUtils.isEmpty(name)) {
                        try {
                            webView.loadUrl(URL + "tags.php?tag=" + URLEncoder.encode(name, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    } else {
                        webView.loadUrl(URL);
                    }
                })
                .show();
    }
}
