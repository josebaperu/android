package com.webview.music;

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
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.CookieManager;
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

import com.webview.music.service.WebViewService;
import com.webview.music.webview.MediaWebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    public final static String RECEIVER = "YOUTUBE_MUSIC";
    /** Watch the transport chain with: adb logcat -s YTM:D */
    private static final String TAG = "YTM";
    private final static String BASE_URL = "https://music.youtube.com/";
    private final static int NOTIFICATION_PERMISSION_REQUEST = 1;

    private MediaWebView mWebView;
    private String script;
    private String playback;
    private String next;
    private String previous;
    private String toggle;
    private String playScript;
    private String pauseScript;
    private BroadcastReceiver receiver;
    /** Written from the WebView's JS thread, read from the main thread. */
    private volatile boolean playing;

    private void startService() {
        Intent serviceIntent = new Intent(this, WebViewService.class);
        serviceIntent.setAction("START");
        ContextCompat.startForegroundService(this, serviceIntent);
        // API 34+ requires an explicit export flag on every runtime-registered receiver.
        ContextCompat.registerReceiver(this, receiver, new IntentFilter(RECEIVER),
                ContextCompat.RECEIVER_NOT_EXPORTED);
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

    /** FLAG_FULLSCREEN is a no-op since API 30; system bars go through the insets controller. */
    private void goFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    /** Wraps a raw script so its declarations stay out of the page's global scope. */
    private static String iife(String js) {
        return "(function() {" + js + "\n})();";
    }

    /** evaluateJavascript avoids the percent/fragment decoding a javascript: URL suffers. */
    private void runScript(String js) {
        if (mWebView != null) {
            mWebView.evaluateJavascript(iife(js), null);
        }
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
        script = fileToStr(R.raw.script);
        playback = fileToStr(R.raw.playback);
        toggle = fileToStr(R.raw.toggle);
        playScript = fileToStr(R.raw.play);
        pauseScript = fileToStr(R.raw.pause);
        next = fileToStr(R.raw.next);
        previous = fileToStr(R.raw.previous);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String extra = intent.getStringExtra("ACTION");
                Log.d(TAG, "act: <- service " + extra);
                if ("TOGGLE".equals(extra)) {
                    runScript(toggle);
                }
                if ("PLAY".equals(extra)) {
                    runScript(playScript);
                }
                if ("PAUSE".equals(extra)) {
                    runScript(pauseScript);
                }
                if ("NEXT".equals(extra)) {
                    runScript(next);
                }
                if ("PREVIOUS".equals(extra)) {
                    runScript(previous);
                }
                if ("DESTROY".equals(extra)) {
                    quit();
                }
            }
        };

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        goFullscreen();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        }
        requestNotificationPermission();
        startService();
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
                runScript(script);
                runScript(playback);
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
                goFullscreen();
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

        // FORCE_DARK is deprecated and reports unsupported on current WebViews.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webSettings, true);
        }
        // onPageFinished runs after the player response is already parsed,
        // so the ad blocker also goes in at document start where that is supported.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            Set<String> youtubeOrigins = Collections.singleton("https://*.youtube.com");
            WebViewCompat.addDocumentStartJavaScript(mWebView, iife(script), youtubeOrigins);
            WebViewCompat.addDocumentStartJavaScript(mWebView, iife(playback), youtubeOrigins);
        }
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        mWebView.setScrollbarFadingEnabled(false);
        registerBackHandler();
        mWebView.addJavascriptInterface(new PlaybackBridge(), "AndroidPlayback");
        mWebView.loadUrl(getValue("url"));
    }

    /**
     * Lets the page tell us whether audio is actually rolling, so the media session
     * and the notification's play/pause button reflect reality instead of a guess.
     */
    private class PlaybackBridge {
        @JavascriptInterface
        public void setPlaying(boolean isPlaying) {
            if (playing == isPlaying) {
                return;
            }
            playing = isPlaying;
            Log.d(TAG, "act: page reports " + (isPlaying ? "PLAYING" : "PAUSED"));
            Intent stateIntent = new Intent(MainActivity.this, WebViewService.class);
            stateIntent.setAction(isPlaying ? "STATE_PLAYING" : "STATE_PAUSED");
            ContextCompat.startForegroundService(MainActivity.this, stateIntent);
        }

        @JavascriptInterface
        public void setTrack(String author, String title) {
            Intent metaIntent = new Intent(MainActivity.this, WebViewService.class);
            metaIntent.setAction("METADATA");
            metaIntent.putExtra("AUTHOR", author != null ? author : "");
            metaIntent.putExtra("TITLE", title != null ? title : "");
            ContextCompat.startForegroundService(MainActivity.this, metaIntent);
        }
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
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST
                && (grantResults.length == 0
                || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Log.w(TAG, "notifications denied; playback controls will be hidden");
            Toast.makeText(this, R.string.notifications_denied, Toast.LENGTH_LONG).show();
        }
    }

    private void registerBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mWebView != null && mWebView.canGoBack()) {
                    mWebView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
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

    private String fileToStr(int resource) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                getResources().openRawResource(resource), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            Log.e(TAG, "could not read raw resource " + resource, e);
        }
        return sb.toString();
    }
}
