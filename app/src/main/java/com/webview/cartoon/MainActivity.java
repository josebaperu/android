package com.webview.cartoon;

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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.webview.cartoon.webview.MediaWebView;

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
    List<String> blacklist;

    Activity mainActivity = this; // If you are in activity
    public final static String RECEIVER = "series";

    private final static String BASE_URL = "https://watchcartoonsonline.eu/";

    private final static String UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.60 Mobile Safari/537.36";

    private final static String TAG = "MainActivity";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        blacklist = getBlackList();
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
            public void onLoadResource(WebView view, String url) {
                applyStylesLoaded();
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
                for(String blackListedWord : blacklist) {
                    if(url.contains(blackListedWord)){
                        isAllowed = false;
                        Log.i(TAG, "ALLOWED_TRUE " + url );
                        break;
                    } else {
                        isAllowed = true;
                        Log.i(TAG, "ALLOWED_FALSE " + url );
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
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setAppCacheEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);   // Enable this only if you want pop-ups!
        webSettings.setMediaPlaybackRequiresUserGesture(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setUserAgentString(UA);
        webSettings.setBlockNetworkLoads(false);
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webSettings, WebSettingsCompat.FORCE_DARK_ON);
        }
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

    private List<String> getBlackList() {
        String line = "";
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
    public void onDestroy() {
        finishAndRemoveTask();
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
        return getSharedPreferences().getString(key, BASE_URL);
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void applyStyles() {
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('#main_header').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('.headitems.register_active').style.float = 'left';})()");

        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div.sidebar.right.scrolling').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('#info h2').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('#info span').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('#info .breadcrumbs').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('#comments').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('#arc-widget-container').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('.content.right').style.minWidth = '100%';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('.wp-content').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('.dt_social_single').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div.control').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelectorAll('div.custom_fields').forEach(c => c.remove());})()");

        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div.dtuser').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('#s').removeAttribute('placeholder');})()");

        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('footer').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('body').style.backgroundColor = 'black';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('.content.right').style.backgroundColor = 'black';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('div.control').style.backgroundColor = 'black';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('header.responsive .search a').style.opacity = '0.25';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('h1').style.opacity = '0.25';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('a.aresp.nav-resp').style.opacity = '0.25';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('#playex').style.marginBottom = '2rem';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('#playex').style.marginTop = '4rem';})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('iframe#arc-widget-launcher-iframe').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('iframe#arc-broker').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('iframe#google_esf').remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelector('iframe#aswift_0').remove();})()");
    }

    private void applyStylesLoaded() {
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelectorAll('div.logo')[0].remove();})()");
        mWebView.loadUrl("javascript:(function() { " +
                "document.querySelectorAll('div.logo')[1].remove();})()");
    }
}
