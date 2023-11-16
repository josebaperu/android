package com.webview.telegratishd;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.webview.telegratishd.webview.MediaWebView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final static WebResourceResponse webResourceResponse = new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    MediaWebView mWebView;
    List<String> blacklistedKeyword;
    Activity mainActivity = this; // If you are in activity
    private final static String BASE_URL = "https://telegratishd.com/";
    private final static String TAG = "MainActivity";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        blacklistedKeyword = getBlackListKeywords();
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
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.hide();
        setContentView(R.layout.activity_main);

        mWebView = new MediaWebView(MainActivity.this);
        mWebView = findViewById(R.id.activity_main_webview);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading (WebView view, String url) {
                if (url.contains("telegratishd")) {
                    return false;
                }
                return true;
            }
            @Override
            public void onLoadResource(WebView view, String url) {
                mWebView.loadUrl("javascript:(function() { " +
                        "document.querySelector('div.block-title h2').remove();})()");
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                applyStyles(url);
            }
            @Override
            public void doUpdateVisitedHistory (WebView view,
                                                String url,
                                                boolean isReload) {
                saveCurrentUrl(url);
            }
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                boolean isAllowed = false;
                for(String blacklistWord : blacklistedKeyword) {
                    if(url.contains(blacklistWord)){
                        isAllowed = false;
                        break;
                    } else {
                        isAllowed = true;
                    }
                }
                Log.i(TAG, isAllowed? "ALLOWED_TRUE ":"ALLOWED_FALSE " + url );
                return isAllowed ? super.shouldInterceptRequest(view, request) :  webResourceResponse;
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
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);   // Enable this only if you want pop-ups!
        webSettings.setMediaPlaybackRequiresUserGesture(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setBlockNetworkLoads(false);
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
        mWebView.loadUrl(getValue());

    }

    private List<String> getBlackListKeywords() {
        String line;
        List<String> list = new ArrayList<>();

        InputStream is = this.getResources().openRawResource(R.raw.blacklist);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        try {
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && !getValue().equals(BASE_URL)) {
            mWebView.loadUrl(BASE_URL);
            save(BASE_URL);
        } else {
            super.onBackPressed();
        }
    }
    @Override
    public void onPause() {
        finish();
        super.onPause();
    }
    private void save(String value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString("url", value);
        editor.apply();
    }
    private void saveCurrentUrl (String url) {
        save(url);
    }

    private String getValue() {
        return getSharedPreferences().getString("url", BASE_URL);
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }
    private void applyStyles(String url) {
        if(isHomePage(url)) {
            mWebView.loadUrl("javascript:(function() { " +
                    "document.querySelector('input#searchbox').remove();})()");
            mWebView.loadUrl("javascript:(function() { " +
                    "const elements = document.querySelector('#canales.row');[0,1,2,3].forEach( i => elements.querySelector('div').remove());})()");
        } else {
            mWebView.loadUrl("javascript:(function() { " +
                    "document.querySelector('a.btn.btn-secondary').remove();})()");
            mWebView.loadUrl("javascript:(function() { " +
                    "NodeList.prototype.forEach = Array.prototype.forEach;document.querySelectorAll('a.button').forEach(function(el) {el.style.color = 'grey'; el.classList.remove('button')});})()");
        }

        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('body').style.backgroundColor = 'black';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('header').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div.footer').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('html body center p').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div.section.mt-2').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div#page_container').style.padding = 'unset';})()");

        mWebView.loadUrl("javascript:(function() { " +
                "setTimeout(() => {document.querySelector('div.widget-visible').remove()}, 5000);})()");
        mWebView.loadUrl("javascript:(function() { " +
                "NodeList.prototype.forEach = Array.prototype.forEach;document.querySelectorAll('a.btn.btn-md').forEach(function(el) {el.classList.remove('btn');el.classList.remove('btn-md')});})()");

        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('main.container').classList = '';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('.items-end').classList = '';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('.grid').classList = '';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('#page_container').classList = '';})()");

    }

    private boolean isHomePage(String url){
        return BASE_URL.equals(url);
    }
}

