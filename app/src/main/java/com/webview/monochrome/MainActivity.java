package com.webview.monochrome;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.webview.monochrome.service.WebViewService;
import com.webview.monochrome.webview.MediaWebView;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    public final static String RECEIVER = "MONOCHROME";
    /** Watch the transport chain with: adb logcat -s YTM:D */
    private static final String TAG = "YTM";
    private final static String BASE_URL = "https://monochrome.tf/";
    private final static int NOTIFICATION_PERMISSION_REQUEST = 1;

    private MediaWebView mWebView;
    private BroadcastReceiver receiver;
    /** Written from the WebView's JS thread, read from the main thread. */
    private volatile boolean playing;

    private void registerPlaybackReceiver() {
        // API 34+ requires an explicit export flag on every runtime-registered receiver.
        ContextCompat.registerReceiver(this, receiver, new IntentFilter(RECEIVER),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    /** Keep the media notification up for as long as this activity's process is alive. */
    private void startPlaybackService() {
        startPlaybackService("START");
    }

    private void startPlaybackService(String action) {
        Intent serviceIntent = new Intent(this, WebViewService.class);
        serviceIntent.setAction(action);
        startPlaybackService(serviceIntent);
    }

    private void startPlaybackService(Intent serviceIntent) {
        if (isFinishing()) {
            return;
        }
        try {
            ContextCompat.startForegroundService(this, serviceIntent);
        } catch (IllegalStateException e) {
            Log.w(TAG, "act: startForegroundService failed; falling back", e);
            startService(serviceIntent);
        }
    }

    /** The media notification is the only playback control, so ask for it up front. */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    /** Used only for HTML5 fullscreen video, not for the normal browsing UI. */
    private void goFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    private void showSystemBars() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.show(WindowInsetsCompat.Type.systemBars());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
    }

    private static String iife(String js) {
        return "(function() {" + js + "\n})();";
    }

    private void runScript(String js) {
        if (mWebView != null) {
            mWebView.evaluateJavascript(iife(js), null);
        }
    }

    private void runCommand(String fn) {
        runScript("if (window." + fn + ") window." + fn + "();");
    }

    /** The user asked to quit from the notification. onDestroy() does the teardown. */
    private void quit() {
        finishAndRemoveTask();
    }

    /** Detaching and destroying the WebView is what stops playback. */
    private void releaseWebView() {
        if (mWebView == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) mWebView.getParent();
        if (parent != null) {
            parent.removeView(mWebView);
        }
        mWebView.destroy();
        mWebView = null;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String extra = intent.getStringExtra("ACTION");
                Log.d(TAG, "act: <- service " + extra);
                if ("TOGGLE".equals(extra)) {
                    runCommand("__monoToggle");
                }
                if ("PLAY".equals(extra)) {
                    runCommand("__monoPlay");
                }
                if ("PAUSE".equals(extra)) {
                    runCommand("__monoPause");
                }
                if ("NEXT".equals(extra)) {
                    runCommand("__monoNext");
                }
                if ("PREVIOUS".equals(extra)) {
                    runCommand("__monoPrev");
                }
                if ("DESTROY".equals(extra)) {
                    quit();
                }
            }
        };

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        }
        requestNotificationPermission();
        registerPlaybackReceiver();
        startPlaybackService();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_main);

        mWebView = findViewById(R.id.activity_main_webview);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void doUpdateVisitedHistory(WebView view,
                                               String url,
                                               boolean isReload) {
                saveCurrentUrl(url);
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                // Returning true stops the system killing our process. Rebuild the
                // activity so playback resumes where it left off instead of quitting.
                if (view != mWebView) {
                    return false;
                }
                Log.w(TAG, "webview renderer gone; rebuilding activity");
                if (mWebView != null) {
                    String url = mWebView.getUrl();
                    if (url != null) {
                        save("url", url);
                    }
                }
                releaseWebView();
                recreate();
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                runScript(PlaybackJs.HOOK);
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient() {
            private View mCustomView;
            private WebChromeClient.CustomViewCallback mCustomViewCallback;
            private int mOriginalOrientation;

            @Override
            public void onHideCustomView() {
                if (mCustomView != null) {
                    ViewGroup parent = (ViewGroup) mCustomView.getParent();
                    if (parent != null) {
                        parent.removeView(mCustomView);
                    }
                    mCustomView = null;
                }
                showSystemBars();
                MainActivity.this.setRequestedOrientation(mOriginalOrientation);
                if (mCustomViewCallback != null) {
                    mCustomViewCallback.onCustomViewHidden();
                    mCustomViewCallback = null;
                }
            }

            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                if (mCustomView != null) {
                    onHideCustomView();
                    return;
                }
                mCustomView = view;
                mOriginalOrientation = MainActivity.this.getRequestedOrientation();
                mCustomViewCallback = callback;
                ((FrameLayout) MainActivity.this.getWindow().getDecorView())
                        .addView(mCustomView, new FrameLayout.LayoutParams(-1, -1));
                goFullscreen();
            }
        });
        CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView, true);
        WebSettings webSettings = mWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // FORCE_DARK is deprecated and reports unsupported on current WebViews.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webSettings, true);
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            Set<String> origins = new HashSet<>();
            origins.add("https://monochrome.tf");
            origins.add("https://*.monochrome.tf");
            WebViewCompat.addDocumentStartJavaScript(mWebView, iife(PlaybackJs.HOOK), origins);
        }
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        mWebView.setScrollbarFadingEnabled(false);
        registerBackHandler();
        mWebView.addJavascriptInterface(new PlaybackBridge(), "AndroidPlayback");
        mWebView.loadUrl(getValue("url"));
    }

    private class PlaybackBridge {
        @JavascriptInterface
        public void setPlaying(boolean isPlaying) {
            if (playing == isPlaying) {
                return;
            }
            playing = isPlaying;
            Log.d(TAG, "act: page reports " + (isPlaying ? "PLAYING" : "PAUSED"));
            startPlaybackService(isPlaying ? "STATE_PLAYING" : "STATE_PAUSED");
        }

        @JavascriptInterface
        public void setTrack(String author, String title) {
            Intent metaIntent = new Intent(MainActivity.this, WebViewService.class);
            metaIntent.setAction("METADATA");
            metaIntent.putExtra("AUTHOR", author != null ? author : "");
            metaIntent.putExtra("TITLE", title != null ? title : "");
            startPlaybackService(metaIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPlaybackService();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    @Override
    protected void onDestroy() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        if (mWebView != null) {
            String url = mWebView.getUrl();
            if (url != null) {
                save("url", url);
            }
        }
        // Destroying the WebView stops audio, so killing the process is unnecessary —
        // System.exit() here also took down the service on any activity recreation.
        releaseWebView();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPlaybackService();
            } else {
                Log.w(TAG, "notifications denied; playback controls will be hidden");
                Toast.makeText(this, R.string.notifications_denied, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void registerBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mWebView != null && mWebView.canGoBack()) {
                    mWebView.goBack();
                } else {
                    // Home, not finish: Samsung kills the process when the
                    // root activity is finished, which also drops playback.
                    moveTaskToBack(true);
                }
            }
        });
    }

    private void saveCurrentUrl(String url) {
        save("url", url);
    }

    private void save(String key, String value) {
        SharedPreferences.Editor editor = prefs().edit();
        editor.putString(key, value);
        editor.apply();
    }

    private String getValue(String key) {
        return prefs().getString(key, BASE_URL);
    }

    /**
     * Same file android.preference.PreferenceManager used, so the saved URL carries
     * over from previous installs now that the deprecated class is gone.
     */
    private SharedPreferences prefs() {
        return getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
    }
}
