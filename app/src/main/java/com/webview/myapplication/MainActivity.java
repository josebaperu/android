package com.webview.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
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

import com.webview.myapplication.service.WebViewService;
import com.webview.myapplication.webview.MediaWebView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    MediaWebView mWebView;
    StringBuilder adservers;

    Activity mainActivity = this; // If you are in activity

    private void startService() {
        Intent serviceIntent = new Intent(this, WebViewService.class);
        serviceIntent.setAction("START");
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black, this.getTheme()));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }
        startService();
        readAdServers();
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_main);

        mWebView = new MediaWebView(MainActivity.this);
        mWebView = findViewById(R.id.activity_main_webview);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mWebView.loadUrl("javascript:(function() { " +
                        "document.querySelector('body').style.backgroundColor = 'black';})()");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mWebView.loadUrl("javascript:(function() { " +
                        "document.querySelector('div#header').style.display = 'none';})()");
                mWebView.loadUrl("javascript:(function() { " +
                        "document.querySelector('.clean-gray').style.display = 'none';})()");
                mWebView.loadUrl("javascript:(function() { " +
                        "document.querySelector('.card-description').style.display = 'none';})()");
                mWebView.loadUrl("javascript:(function() { " +
                        "document.querySelector('nav').style.display = 'none';})()");
                mWebView.loadUrl("javascript:(function() { " +
                        "document.querySelector('div.footer').style.display = 'none';})()");
                mWebView.loadUrl("javascript:(function() { " +
                        "document.querySelector('#buscar').style.display = 'none';})()");
                mWebView.loadUrl("javascript:(function() { " +
                        "document.querySelector('h1').style.display = 'none';})()");
                mWebView.loadUrl("javascript:(function() { " +
                        "document.querySelector('h3').style.display = 'none';})()");
                mWebView.loadUrl("javascript:(function() { " +
                        "document.querySelector('body').style.backgroundColor = 'black';})()");
//              mWebView.loadUrl("javascript:(function() { " +
//                      "document.querySelector('body').setAttribute('data-theme','dark');})()");
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

            public void onHideCustomView() {
                ((FrameLayout) mainActivity.getWindow().getDecorView()).removeView(this.mCustomView);
                this.mCustomView = null;
                mainActivity.getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
                mainActivity.setRequestedOrientation(this.mOriginalOrientation);
                this.mCustomViewCallback.onCustomViewHidden();
                this.mCustomViewCallback = null;
            }

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
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setBlockNetworkLoads(false);

        if (savedInstanceState == null) {
            mWebView.loadUrl("https://canales.online/");
        }

    }

    private void readAdServers() {
        String line = "";
        adservers = new StringBuilder();

        InputStream is = this.getResources().openRawResource(R.raw.adblockserverlist);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        if (is != null) {
            try {
                while ((line = br.readLine()) != null) {
                    adservers.append(line);
                    adservers.append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWebView.restoreState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack())
            mWebView.goBack();// if there is previous page open it
        else
            super.onBackPressed();//if there is no previous page, close app
    }
}