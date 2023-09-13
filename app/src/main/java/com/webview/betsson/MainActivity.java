package com.webview.betsson;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
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
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.webview.betsson.webview.MediaWebView;

import java.io.ByteArrayInputStream;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {
    private MediaWebView mWebView;
    private final Activity mainActivity = this; // If you are in activity
    private final static String BASE_URL = "https://www.betsson.com/pe/";
    private FloatingActionButton floatingActionButton;
    private FloatingActionButton floatingActionButtonFavorite;
    private FloatingActionButton floatingActionButtonLive;
    private FloatingActionButton floatingActionButtonGames;
    private final static WebResourceResponse webResourceResponse = new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));

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
        mWebView = findViewById(R.id.activity_main_webview);
        floatingActionButton = findViewById(R.id.fab_button);
        floatingActionButtonFavorite = findViewById(R.id.fav_button);
        floatingActionButtonLive = findViewById(R.id.live);
        floatingActionButtonGames = findViewById(R.id.games);
        floatingActionButtonLive.setOnClickListener(v-> {
            mWebView.loadUrl("https://www.betsson.com/pe/apuestas-deportivas");
        });
        floatingActionButtonGames.setOnClickListener(v -> {
            mWebView.loadUrl("https://www.betsson.com/pe/apuestas-deportivas/futbol");
        });

        floatingActionButtonFavorite .setOnClickListener(v-> {
            mWebView.loadUrl("https://www.betsson.com/pe/apuestas-deportivas/en-vivo/futbol");
        });
        floatingActionButton.setOnClickListener(v -> {
            mWebView.loadUrl("https://www.betsson.com/pe/apuestas-deportivas/historial-de-apuestas");
        });
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void doUpdateVisitedHistory (WebView view,
                                                String url,
                                                boolean isReload) {
                saveCurrentUrl(url);
            }
            @Override
            public void onLoadResource(WebView view, String url) {
                applyStyles();
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {}
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                boolean isAllowed = false;
                for(String blacklistWord : Arrays.asList(".png",".jpeg",".gif",".webp",".jpg")) {
                    if(url.contains(blacklistWord)){
                        isAllowed = false;
                        break;
                    } else {
                        isAllowed = true;
                    }
                }
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
        webSettings.setSavePassword(true);
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webSettings, WebSettingsCompat.FORCE_DARK_ON);
        }
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        mWebView.setScrollbarFadingEnabled(false);

        mWebView.loadUrl(getValue("url"));

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()){
            mWebView.goBack();// if there is previous page open it
        } else {
            super.onBackPressed();//if there is no previous page, close app
        }
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
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('body > obg-app-root > mat-sidenav-container > mat-sidenav-content > obg-m-sm-betting-layout-container > obg-m-sm-sportsbook-layout-container > header > obg-smart-app-banner').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('section.obg-footer-responsible-gaming').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('footer').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('section.obg-m-play-with-us-section.ng-star-inserted').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('.obg-m-reassurance-section-wrapper.ng-star-inserted').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('.obg-m-faq-section.ng-star-inserted').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('.obg-m-seo-section.ng-star-inserted').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div.promotion').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div.responsive').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('body > obg-app-root > mat-sidenav-container > mat-sidenav-content > obg-app-core-layout-container > div > obg-m-home-wrapper-container > obg-lazy-loader > obg-m-home-page-v2-container > obg-m-preview-section:nth-child(5) > section').style.display = 'none';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('body > obg-app-root > mat-sidenav-container > mat-sidenav-content > obg-app-core-layout-container > div > obg-m-home-wrapper-container > obg-lazy-loader > obg-m-home-page-v2-container > obg-m-preview-section:nth-child(6) > section').style.display = 'none';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div.obg-footer-container').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('body > obg-app-root > mat-sidenav-container > mat-sidenav-content > obg-m-sm-betting-layout-container > obg-m-sm-sportsbook-layout-container > div > obg-m-sm-sportsbook-lobby-container > obg-m-sportsbook-lobby-container > obg-content-links > div').remove();})()");
    }
}
