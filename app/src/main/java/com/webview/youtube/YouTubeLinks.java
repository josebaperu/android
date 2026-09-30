package com.webview.youtube;

import android.content.Intent;
import android.net.Uri;

import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a link handed over by a browser (or the share sheet) into an https URL
 * the WebView can load. Anything that is not a YouTube URL is rejected so a
 * crafted share cannot navigate the WebView off YouTube.
 */
public final class YouTubeLinks {

    private static final Pattern URL_IN_TEXT = Pattern.compile(
            "(?i)(?:https?://|vnd\\.youtube:|youtube:|intent:)\\S+");
    private static final Pattern VIDEO_ID = Pattern.compile("[0-9A-Za-z_-]{11}");
    private static final Pattern PLAYLIST_ID = Pattern.compile("[0-9A-Za-z_-]{10,}");

    private YouTubeLinks() {
    }

    /**
     * YouTube page to open for this VIEW or SEND intent, or null when the
     * intent is a normal launch or doesn't carry a YouTube link.
     */
    public static String urlFrom(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return null;
        }
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            return urlFromView(intent.getData());
        }
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            CharSequence shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            return shared == null ? null : firstInText(shared.toString());
        }
        return null;
    }

    /** https URL for a YouTube URI, or null when it isn't one. */
    public static String toWebUrl(Uri uri) {
        if (uri == null || uri.getScheme() == null) {
            return null;
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if ("http".equals(scheme) || "https".equals(scheme)) {
            if (!isYouTubeHost(uri.getHost())) {
                return null;
            }
            return https(uri);
        }
        if ("vnd.youtube".equals(scheme) || "youtube".equals(scheme)) {
            return fromCustomScheme(uri);
        }
        return null;
    }

    /**
     * YouTube's own pages sometimes navigate to intent:// so the official app
     * (or, failing that, the browser) takes over. Pull the https URL back out
     * so playback stays here.
     */
    public static String fromIntentUri(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            Intent intent = Intent.parseUri(raw, Intent.URI_INTENT_SCHEME);
            Uri data = intent.getData();
            if (data != null && (data.getScheme() == null
                    || !"intent".equalsIgnoreCase(data.getScheme()))) {
                String url = toWebUrl(data);
                if (url != null) {
                    return url;
                }
            }
            String fallback = intent.getStringExtra("browser_fallback_url");
            return fallback == null ? null : toWebUrl(Uri.parse(fallback));
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    private static String urlFromView(Uri data) {
        if (data == null) {
            return null;
        }
        if ("intent".equalsIgnoreCase(data.getScheme())) {
            return fromIntentUri(data.toString());
        }
        return toWebUrl(data);
    }

    private static String firstInText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        Matcher matcher = URL_IN_TEXT.matcher(text);
        while (matcher.find()) {
            String candidate = trimTail(matcher.group());
            String url = "intent".equalsIgnoreCase(schemeOf(candidate))
                    ? fromIntentUri(candidate)
                    : toWebUrl(Uri.parse(candidate));
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private static String schemeOf(String raw) {
        int colon = raw.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        return raw.substring(0, colon);
    }

    private static String fromCustomScheme(Uri uri) {
        if (uri.isHierarchical() && isYouTubeHost(uri.getHost())) {
            return https(uri);
        }
        String videoId = queryParam(uri, "v");
        if (isVideoId(videoId)) {
            return "https://www.youtube.com/watch?v=" + videoId;
        }
        String listId = queryParam(uri, "list");
        if (listId != null && PLAYLIST_ID.matcher(listId).matches()) {
            return "https://www.youtube.com/playlist?list=" + listId;
        }
        String bare = bareId(uri);
        if (isVideoId(bare)) {
            return "https://www.youtube.com/watch?v=" + bare;
        }
        return null;
    }

    /** Video id sitting where a host or opaque scheme-specific part would be. */
    private static String bareId(Uri uri) {
        if (uri.isHierarchical()) {
            String path = uri.getPath();
            boolean noPath = path == null || path.isEmpty() || "/".equals(path);
            if (noPath) {
                return uri.getHost();
            }
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (!path.contains("/")) {
                return path;
            }
            return null;
        }
        String ssp = uri.getSchemeSpecificPart();
        if (ssp == null) {
            return null;
        }
        if (ssp.startsWith("//")) {
            ssp = ssp.substring(2);
        }
        int query = ssp.indexOf('?');
        if (query >= 0) {
            ssp = ssp.substring(0, query);
        }
        int slash = ssp.indexOf('/');
        if (slash >= 0) {
            ssp = ssp.substring(0, slash);
        }
        return ssp;
    }

    private static String queryParam(Uri uri, String name) {
        if (uri.isHierarchical()) {
            try {
                String value = uri.getQueryParameter(name);
                if (value != null) {
                    return value;
                }
            } catch (UnsupportedOperationException ignored) {
                // Opaque URIs have no query accessor; fall through to the raw text.
            }
        }
        String ssp = uri.getSchemeSpecificPart();
        if (ssp == null) {
            return null;
        }
        int query = ssp.indexOf('?');
        if (query < 0) {
            return null;
        }
        String prefix = name + "=";
        for (String part : ssp.substring(query + 1).split("&")) {
            if (part.startsWith(prefix)) {
                return Uri.decode(part.substring(prefix.length()));
            }
        }
        return null;
    }

    private static String https(Uri uri) {
        if (uri.getUserInfo() != null) {
            return null;
        }
        int port = uri.getPort();
        if (port != -1 && port != 80 && port != 443) {
            return null;
        }
        String host = uri.getHost();
        if (host == null) {
            return null;
        }
        return uri.buildUpon().scheme("https").encodedAuthority(host).build().toString();
    }

    private static boolean isYouTubeHost(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        host = host.toLowerCase(Locale.ROOT);
        if (host.startsWith("www.")) {
            String rest = host.substring(4);
            if ("youtube.com".equals(rest) || "youtu.be".equals(rest)
                    || "youtube-nocookie.com".equals(rest)) {
                return true;
            }
        }
        return "youtube.com".equals(host)
                || "youtu.be".equals(host)
                || "youtube-nocookie.com".equals(host)
                || host.endsWith(".youtube.com")
                || host.endsWith(".youtube-nocookie.com");
    }

    private static boolean isVideoId(String value) {
        return value != null && VIDEO_ID.matcher(value).matches();
    }

    /** Shares often glue a closing paren or period onto the URL. */
    private static String trimTail(String raw) {
        int end = raw.length();
        while (end > 0 && isTailPunctuation(raw.charAt(end - 1))) {
            end--;
        }
        return raw.substring(0, end);
    }

    private static boolean isTailPunctuation(char c) {
        return c == '.' || c == ',' || c == ';' || c == ')' || c == ']'
                || c == '!' || c == '?' || c == '\'' || c == '"';
    }
}
