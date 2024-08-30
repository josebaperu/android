package com.webview.openspotify;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
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
import androidx.core.content.ContextCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.webview.openspotify.receiver.ActionReceiver;
import com.webview.openspotify.service.WebViewService;
import com.webview.openspotify.webview.MediaWebView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static WebResourceResponse webResourceResponse = new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    private MediaWebView mWebView;
    private final Activity mainActivity = this;
    private final static String UA = "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36";
    public final static String RECEIVER = "OPENSPOTIFY";
    private final static String BASE_URL = "https://open.spotify.com/";
    private String script;
    private String toggle;
    private String next;
    private String open;
    private ActionReceiver receiver;

    private void startService() {
        Intent serviceIntent = new Intent(this, WebViewService.class);
        serviceIntent.setAction("START");
        ContextCompat.startForegroundService(this, serviceIntent);
        registerReceiver(receiver, new IntentFilter(RECEIVER));
    }

    private void handleObserverDestroy() {
        saveUrl(mWebView.getUrl());
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        finishAndRemoveTask();
        receiver = null;
    }

    private void sendToService(String msg) {
        Intent serviceIntent = new Intent(this, WebViewService.class);
        serviceIntent.setAction("PLAYING");
        serviceIntent.putExtra("PLAYING", msg);
        startService(serviceIntent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mWebView.loadUrl("javascript:(function() { " +
                open +
                ";})()");
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        script = fileToString(R.raw.script);
        toggle = fileToString(R.raw.toggle);
        next = fileToString(R.raw.next);
        open = fileToString(R.raw.open);
        receiver = new ActionReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String extra = intent.getStringExtra("ACTION");
                if ("TOGGLE".equals(extra)) {
                    mWebView.loadUrl("javascript:(function() { " +
                            toggle +
                            ";})()");
                }
                if ("NEXT".equals(extra)) {
                    mWebView.loadUrl("javascript:(function() { " +
                            next +
                            ";})()");
                }
                if ("DESTROY".equals(extra)) {
                    handleObserverDestroy();
                }

            }
        };
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black, this.getTheme()));
        } else {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }
        startService();

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_main);

        mWebView = new MediaWebView(MainActivity.this);
        mWebView = findViewById(R.id.activity_main_webview);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mWebView.loadUrl("javascript:(function() { " +
                        script +
                        ";})()");

            }

            @Override
            public void doUpdateVisitedHistory(WebView view,
                                               String url,
                                               boolean isReload) {
                saveUrl(url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                boolean isAllowed = false;

                List<String> whiteList = Arrays.asList("spotify", "scdn", "google", "facebook", "apple", "recaptcha");
                for (String whiteListHost : whiteList) {
                    if (url.contains(whiteListHost)) {
                        isAllowed = true;
                        break;
                    }
                }

                return isAllowed ? super.shouldInterceptRequest(view, request) : webResourceResponse;
            }
        });


        mWebView.setWebChromeClient(new WebChromeClient() {
            private View mCustomView;
            private WebChromeClient.CustomViewCallback mCustomViewCallback;
            private int mOriginalOrientation;
            private int mOriginalSystemUiVisibility;

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String message = consoleMessage.message();
                Log.i("JS_CONSOLE", message);
                if (message.contains("Playing")) {
                    String msg = consoleMessage.message().substring(10);
                    sendToService(msg);
                }
                return true;
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                String[] resources = request.getResources();
                for (String resource : resources) {
                    if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resource)) {
                        request.grant(resources);
                        return;
                    }
                }

                super.onPermissionRequest(request);
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
        WebSettings webSettings = getWebSettings();
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webSettings, WebSettingsCompat.FORCE_DARK_ON);
        }
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        mWebView.setScrollbarFadingEnabled(false);
        mWebView.loadUrl(getUrl());

    }

    private @NonNull WebSettings getWebSettings() {
        WebSettings webSettings = mWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        //webSettings.setAppCacheEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
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
        return webSettings;
    }

    private String fileToString(int resource) {
        String line = "";
        StringBuilder sb = new StringBuilder();

        InputStream is = this.getResources().openRawResource(resource);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException ignored) {
        }
        return sb.toString();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWebView.restoreState(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handleObserverDestroy();
        System.exit(1);
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack())
            mWebView.goBack();
        else
            super.onBackPressed();
    }

    private void saveUrl(String value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString("url", value);
        editor.apply();
    }

    private String getUrl() {
        return getSharedPreferences().getString("url", BASE_URL);
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

}