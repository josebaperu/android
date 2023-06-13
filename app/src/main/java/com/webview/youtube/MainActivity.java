package com.webview.youtube;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.webview.youtube.service.WebViewService;
import com.webview.youtube.webview.MediaWebView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private MediaWebView mWebView;
    private StringBuilder adservers;
    private StringBuilder youtubeAds;
    private StringBuilder continueWatching;
    private StringBuilder scroll;
    private final Activity mainActivity = this; // If you are in activity
    private final static String UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.60 Mobile Safari/537.36";
    public final static String RECEIVER = "YOUTUBE";
    private boolean isImmersive = false;

    private BroadcastReceiver receiver;


    private void startService() {
        Intent serviceIntent = new Intent(this, WebViewService.class);
        serviceIntent.setAction("START");
        ContextCompat.startForegroundService(this, serviceIntent);
        registerReceiver(receiver, new IntentFilter(RECEIVER));

    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                save("url", mWebView.getUrl());
                unregisterReceiver(receiver);
                finishAndRemoveTask();
            }
        };
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black, this.getTheme()));
        } else {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }
        startService();
        adservers = fileToSb(R.raw.adblockserverlist);
        youtubeAds = fileToSb(R.raw.noadsyoutube);
        continueWatching = fileToSb(R.raw.continuewatching);
        scroll = fileToSb(R.raw.scroll);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_main);

        mWebView = new MediaWebView(MainActivity.this);
        mWebView = findViewById(R.id.activity_main_webview);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mWebView.loadUrl("javascript:(function() { " +
                        youtubeAds.toString() +
                        ";})()");
                mWebView.loadUrl("javascript:(function() { " +
                        continueWatching.toString() +
                        ";})()");
                mWebView.loadUrl("javascript:(function() { " +
                        scroll.toString() +
                        ";})()");
            }
            @Override
            public void doUpdateVisitedHistory (WebView view,
                                                String url,
                                                boolean isReload) {
                saveCurrentUrl(url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
                String kk5 = String.valueOf(adservers);

                if (kk5.contains(":::::" + request.getUrl().getHost())) {
                    return new WebResourceResponse("text/plain", "utf-8", EMPTY);
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient() {
            private View mCustomView;
            private WebChromeClient.CustomViewCallback mCustomViewCallback;
            private int mOriginalOrientation;
            private int mOriginalSystemUiVisibility;

            public Bitmap getDefaultVideoPoster() {
                if (mainActivity == null) {
                    return null;
                }
                return BitmapFactory.decodeResource(mainActivity.getApplicationContext().getResources(), 2130837573);
            }
            @Override
            public void onHideCustomView() {
                ((FrameLayout) mainActivity.getWindow().getDecorView()).removeView(this.mCustomView);
                this.mCustomView = null;
                mainActivity.getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
                mainActivity.setRequestedOrientation(this.mOriginalOrientation);
                this.mCustomViewCallback.onCustomViewHidden();
                this.mCustomViewCallback = null;
            }
            @Override
            public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback paramCustomViewCallback) {
                if (this.mCustomView != null) {
                    onHideCustomView();
                    return;
                }
                this.mCustomView = paramView;
                this.mOriginalSystemUiVisibility = mainActivity.getWindow().getDecorView().getSystemUiVisibility();
                this.mOriginalOrientation = mainActivity.getRequestedOrientation();
                this.mCustomViewCallback = paramCustomViewCallback;
                ((FrameLayout) mainActivity.getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
                mainActivity.getWindow().getDecorView().setSystemUiVisibility(3846);
            }
        });
        CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView, true);
        WebSettings webSettings = mWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);   // Enable this only if you want pop-ups!
        webSettings.setMediaPlaybackRequiresUserGesture(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);


        webSettings.setUserAgentString(UA);
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webSettings, WebSettingsCompat.FORCE_DARK_ON);
        }
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        mWebView.setScrollbarFadingEnabled(false);

        if(isImmersive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
            } else {
                int uiVisibility = mainActivity.getWindow().getDecorView().getSystemUiVisibility();

                uiVisibility |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                uiVisibility |= View.SYSTEM_UI_FLAG_FULLSCREEN;
                uiVisibility |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                uiVisibility |= View.SYSTEM_UI_FLAG_IMMERSIVE;
                uiVisibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                mainActivity.getWindow().getDecorView().setSystemUiVisibility(uiVisibility);
            }
        }
        mWebView.loadUrl(getValue("url"));
    }

    private StringBuilder fileToSb(int resource) {
        String line = "";
        StringBuilder sb = new StringBuilder();

        InputStream is = this.getResources().openRawResource(resource);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        if (is != null) {
            try {
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack())
            mWebView.goBack();// if there is previous page open it
        else
            super.onBackPressed();//if there is no previous page, close app
    }
    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
    @Override
    public void onResume(){
        super.onResume();

    }
    @Override
    public void onStart(){
        super.onStart();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void save(String key, String value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(key, value);
        editor.apply();
    }
    private void saveCurrentUrl (String url) {
        save("url", url);
    }


    private String getValue(String key) {
        return getSharedPreferences().getString(key, "https://www.youtube.com");
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }
}