package com.marteczek.photoreporter.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.Settings;
import com.marteczek.photoreporter.application.data.ImgurUserData;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.marteczek.photoreporter.application.Settings.Debug.D;
import static com.marteczek.photoreporter.application.Settings.Debug.E;

public class ImgurLoginActivity extends AppCompatActivity {
    private static final String TAG = "ImgurLoginActivity";

    protected static final String PATTERN_ACCESS_TOKEN = "access_token=([0-9a-z]+)";
    protected static final String PATTERN_EXPIRES_IN = "expires_in=([0-9]+)";
    protected static final String PATTERN_REFRESH_TOKEN = "refresh_token=([0-9a-z]+)";
    protected static final String PATTERN_ACCOUNT_USERNAME = "account_username=([^&]+)";
    protected static final String PATTERN_ACCOUNT_ID = "account_id=([^&]+)";

    private static final String CURRENT_TIME = "current_time";

    private WebView webView;

    private long currentTime;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imgur_login);
        String client_id = Settings.getClientId(this);
        webView = findViewById(R.id.webView);
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        }
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0(Linux)");
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String strUrl) {
                super.onPageFinished(view, strUrl);
                if(D) Log.d(TAG , "url: " + strUrl);
                try {
                    URL url = new URL(strUrl);

                    String query = url.getQuery();
                    if(query != null && query.equals("state=" +
                            Settings.getClientUUID(ImgurLoginActivity.this))) {
                        String ref = url.getRef();
                        if (ref != null) {
                            String accessToken = null;
                            Long expiresIn = null;
                            String refreshToken = null;
                            String accountUsername = null;
                            String accountId = null;
                            Matcher matcher;
                            matcher = Pattern.compile(PATTERN_ACCESS_TOKEN).matcher(ref);
                            if (matcher.find()) {
                                accessToken = matcher.group(1);
                            }
                            matcher = Pattern.compile(PATTERN_EXPIRES_IN).matcher(ref);
                            if (matcher.find()) {
                                expiresIn = Long.valueOf(matcher.group(1));
                            }
                            matcher = Pattern.compile(PATTERN_REFRESH_TOKEN).matcher(ref);
                            if (matcher.find()) {
                                refreshToken = matcher.group(1);
                            }
                            matcher = Pattern.compile(PATTERN_ACCOUNT_USERNAME).matcher(ref);
                            if (matcher.find()) {
                                accountUsername = matcher.group(1);
                            }
                            matcher = Pattern.compile(PATTERN_ACCOUNT_ID).matcher(ref);
                            if (matcher.find()) {
                                accountId = matcher.group(1);
                            }
                            if( accessToken != null && expiresIn != null && refreshToken != null
                                    && accountUsername != null && accountId  != null) {
                                ImgurUserData userData = ImgurUserData.builder()
                                        .accessToken(accessToken)
                                        .expiresOn(new Date(currentTime + expiresIn * 1000))
                                        .refreshToken(refreshToken)
                                        .accountUsername(accountUsername)
                                        .accountId(accountId)
                                        .build();
                                Settings.setImgurUserData(ImgurLoginActivity.this,
                                        userData);
                                Toast.makeText(ImgurLoginActivity.this,
                                        R.string.account_has_been_added,
                                        Toast.LENGTH_LONG).show();
                                finish();
                            }
                        }
                    }
                } catch (MalformedURLException e) {
                    if(E) Log.e(TAG, "MalformedURLException", e);
                }
            }
        });
        if (savedInstanceState == null) {
            webView.loadUrl("https://api.imgur.com/oauth2/authorize?client_id=" + client_id
                    + "&response_type=token&state=" + Settings.getClientUUID(this));
            currentTime = new Date().getTime();
        } else {
            currentTime = savedInstanceState.getLong(CURRENT_TIME);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
        outState.putLong(CURRENT_TIME, currentTime);
    }
}
