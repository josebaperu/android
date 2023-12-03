package com.webview.series;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.webview.series.webview.MediaWebView;

import java.io.ByteArrayInputStream;

public class MainActivity extends AppCompatActivity {
    private final static WebResourceResponse webResourceResponse = new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    MediaWebView mWebView;
    Activity mainActivity = this; // If you are in activity
    private final static String BASE_URL = "https://serieshd.watch/";
    private final static String UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.60 Mobile Safari/537.36";

    private final static String TAG = "MainActivity";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        actionBar.hide();
        setContentView(R.layout.activity_main);

        mWebView = new MediaWebView(MainActivity.this);
        mWebView = findViewById(R.id.activity_main_webview);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                applyStyles();
            }

            @Override
            public void doUpdateVisitedHistory (WebView view,
                                                String url,
                                                boolean isReload) {
                saveCurrentUrl(url);
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
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setAppCacheEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);   // Enable this only if you want pop-ups!
        webSettings.setMediaPlaybackRequiresUserGesture(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setUserAgentString(UA);
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
        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mWebView.loadUrl(getValue("url"));

    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && !getValue("url").equals(BASE_URL)) {
            mWebView.loadUrl(BASE_URL);
            save("url", BASE_URL);
        } else {
            super.onBackPressed();
        }
    }
    @Override
    public void onPause() {
        finish();
        super.onPause();
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
        return getSharedPreferences().getString(key, BASE_URL);
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void applyStyles() {
/*        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('span.logo-text').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div#header_menu').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('.watching_player-control').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('.alert.mb-3').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('body').style.backgroundColor = 'black!important';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div.dropdown-menu.dropdown-menu-new').className = '';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div.prebreadcrumb').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('#footer').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('#mv-keywords').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div#mv-keywords').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div.elements').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div.film_related.film_related-list').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div#header_right div#user-slot').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div#mobile_menu').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div.detail_page-servers').remove();})()");*/
    }
}