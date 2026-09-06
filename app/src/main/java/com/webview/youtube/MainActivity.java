package com.webview.youtube;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.RenderProcessGoneDetail;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.webview.youtube.receiver.ActionReceiver;
import com.webview.youtube.service.WebViewService;
import com.webview.youtube.webview.MediaWebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private MediaWebView mWebView;
    public final static String RECEIVER = "YOUTUBE";
    /** Watch the whole transport chain with: adb logcat -s YT:D */
    private static final String TAG = "YT";
    private final static String BASE_URL = "https://www.youtube.com/";
    private final static int NOTIFICATION_PERMISSION_REQUEST = 1;
    private String script;
    private String toggle;
    private String next;
    private String css;
    private String playback;
    private String playScript;
    private String pauseScript;
    private ActionReceiver receiver;
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

    /** The user asked to quit from the notification. onDestroy() does the teardown. */
    private void quit() {
        finishAndRemoveTask();
    }

    /**
     * targetSdk 35+ is always edge-to-edge, so the WebView would draw under the
     * status bar and camera cutout unless those insets are applied as padding.
     */
    private void applySystemBarInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View root = findViewById(R.id.activity_main_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    /** Used only for HTML5 fullscreen video, not for the normal browsing UI. */
    private void goFullscreen() {
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
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        script = fileToStr(R.raw.script);
        toggle = fileToStr(R.raw.toggle);
        next = fileToStr(R.raw.next);
        css = fileToStr(R.raw.style);
        playback = fileToStr(R.raw.playback);
        playScript = fileToStr(R.raw.play);
        pauseScript = fileToStr(R.raw.pause);

        receiver = new ActionReceiver() {
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            int black = ContextCompat.getColor(this, R.color.black);
            getWindow().setStatusBarColor(black);
            getWindow().setNavigationBarColor(black);
        }

        requestNotificationPermission();
        startService();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_main);
        applySystemBarInsets();

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
                String url = mWebView.getUrl();
                if (url != null) {
                    save("url", url);
                }
                releaseWebView();
                recreate();
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                injectCSS();
                runScript(script);
                runScript(playback);
            }

            private void injectCSS() {
                String encoded = Base64.encodeToString(
                        css.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                runScript(
                        // Guarded: onPageFinished fires on every navigation, and without
                        // the id check each one appended another <style> to the document.
                        "if (document.getElementById('yt_app_css')) { return; }" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('style');" +
                        "style.id = 'yt_app_css';" +
                        "style.type = 'text/css';" +
                        // textContent, not innerHTML: YouTube sets a Trusted Types policy
                        // that rejects innerHTML assignment outright.
                        "style.textContent = window.atob('" + encoded + "');" +
                        "parent.appendChild(style)");
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient() {
            private View mCustomView;
            private WebChromeClient.CustomViewCallback mCustomViewCallback;
            private int mOriginalOrientation;

            @Override
            public void onHideCustomView() {
                ((FrameLayout) MainActivity.this.getWindow().getDecorView()).removeView(this.mCustomView);
                this.mCustomView = null;
                showSystemBars();
                MainActivity.this.setRequestedOrientation(this.mOriginalOrientation);
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
                this.mOriginalOrientation = MainActivity.this.getRequestedOrientation();
                this.mCustomViewCallback = paramCustomViewCallback;
                ((FrameLayout) MainActivity.this.getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
                goFullscreen();
            }
        });
        CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView, true);
        WebSettings webSettings = mWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
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
        //webSettings.setUserAgentString("Mozilla/5.0 (Windows Phone 10.0; Android 4.2.1; Microsoft; Lumia 640 LTE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Mobile Safari/537.36 Edge/13.10586");

        // FORCE_DARK is deprecated and unsupported on current WebViews; algorithmic
        // darkening is the replacement and follows the app's DayNight theme.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webSettings, true);
        }
        // onPageFinished runs after YouTube has already parsed its player response,
        // so the ad blocker also goes in at document start where that is supported.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            Set<String> youtubeOrigins = Collections.singleton("https://*.youtube.com");
            WebViewCompat.addDocumentStartJavaScript(mWebView, iife(script), youtubeOrigins);
            WebViewCompat.addDocumentStartJavaScript(mWebView, iife(playback), youtubeOrigins);
        }
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        mWebView.setScrollbarFadingEnabled(false);

        // onBackPressed() is no longer invoked once predictive back is on (targetSdk 36+).
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mWebView != null && mWebView.canGoBack()) {
                    mWebView.goBack();  // if there is a previous page open it
                } else {
                    setEnabled(false);  // no previous page, let the system close the app
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        mWebView.addJavascriptInterface(new PlaybackBridge(), "AndroidPlayback");

        mWebView.loadUrl(getValue("url"));
    }

    /**
     * Lets the page tell us whether a video is actually rolling, so the media session
     * and the notification's play/pause button reflect reality instead of a guess.
     */
    private class PlaybackBridge {
        @JavascriptInterface
        public void setPlaying(boolean isPlaying) {
            if (playing == isPlaying) {
                return;
            }
            playing = isPlaying;
            // What the page reports. If this flips to false with no transport command
            // logged before it, the player paused itself rather than being told to.
            Log.d(TAG, "act: page reports " + (isPlaying ? "PLAYING" : "PAUSED"));
            // Tell the service too, so the media session and the notification button
            // show what the video is actually doing.
            Intent stateIntent = new Intent(MainActivity.this, WebViewService.class);
            stateIntent.setAction(isPlaying ? "STATE_PLAYING" : "STATE_PAUSED");
            ContextCompat.startForegroundService(MainActivity.this, stateIntent);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
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

    @Override
    protected void onDestroy() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        if (mWebView != null) {
            save("url", mWebView.getUrl());
        }
        // Destroying the WebView stops audio, so killing the process is unnecessary -
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

    private void save(String key, String value) {
        SharedPreferences.Editor editor = prefs().edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void saveCurrentUrl(String url) {
        save("url", url);
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

    /** Wraps a raw script so its declarations stay out of the page's global scope. */
    private static String iife(String js) {
        return "(function() {" + js + "\n})();";
    }

    private void runScript(String js) {
        if (mWebView != null) {
            mWebView.evaluateJavascript(iife(js), null);
        }
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
