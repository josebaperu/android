package com.webview.monochrome.service;

import static com.webview.monochrome.MainActivity.RECEIVER;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadata;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.IntentCompat;

import com.webview.monochrome.MainActivity;
import com.webview.monochrome.R;

public class WebViewService extends Service {

    /** Watch the transport chain with: adb logcat -s YTM:D */
    private static final String TAG = "YTM";
    private static final String APP_NAME = "MONOCHROME";
    private static final String CHANNEL_ID = APP_NAME + "_CHANNEL_ID";
    private static final String CHANNEL_NAME = APP_NAME + "_CHANNEL_NAME";
    private static final int NOTIFICATION_ID = 1;

    private NotificationManager manager;
    private androidx.media.app.NotificationCompat.MediaStyle mediaStyle;
    private MediaSessionCompat mediaSession;
    private Bitmap ytmIcon;
    private NotificationCompat.Builder builder;
    private PendingIntent deletePendingIntent;
    private PendingIntent togglePendingIntent;
    private PendingIntent nextPendingIntent;
    private PendingIntent previousPendingIntent;
    private String author = "";
    private String title = "";
    /** Real state of the page's audio, reported by the activity's playback bridge.
     *  Null until the first report, so an opening "paused" is still published. */
    private Boolean isPlaying;

    @Override
    public void onCreate() {
        super.onCreate();
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        playbackStateBuilder
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0f);

        ytmIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_monochrome);
        manager = getSystemService(NotificationManager.class);
        mediaSession = new MediaSessionCompat(getApplicationContext(), "YTM:mediaService");
        mediaStyle = new androidx.media.app.NotificationCompat.MediaStyle();
        mediaSession.setCallback(callback);
        mediaSession.setActive(true);
        mediaSession.setPlaybackState(playbackStateBuilder.build());
        mediaStyle.setShowActionsInCompactView(0, 1, 2);
        mediaStyle.setShowCancelButton(true);
        // Do not MediaStyle.setMediaSession(): Samsung One UI then owns the
        // shade entry as Now Playing and hides it when audio pauses or dies.
        // The session stays active for lockscreen and headset controls.
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, ytmIcon)
                .putBitmap(MediaMetadata.METADATA_KEY_ART, ytmIcon)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, -1L)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "playback")
                .putString(MediaMetadata.METADATA_KEY_TITLE, "monochrome")
                .putString(MediaMetadata.METADATA_KEY_ALBUM, "monochrome")
                .build());
    }

    @SuppressLint("InlinedApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent != null ? intent.getAction() : null;
        Log.d(TAG, "svc: command " + action + " (startId=" + startId + ")");
        if ("DESTROY".equals(action)) {
            destroyService();
            return START_NOT_STICKY;
        }
        // Sticky restart (null intent) and every later command must show the
        // notification. startForegroundService() also requires startForeground()
        // even when playback state did not change.
        ensureNotification();
        if (action == null) {
            return START_STICKY;
        }
        switch (action) {
            case "TOGGLE":
                sendMessageToActivity("TOGGLE");
                break;
            case "PLAY":
                sendMessageToActivity("PLAY");
                break;
            case "PAUSE":
                sendMessageToActivity("PAUSE");
                break;
            case "STATE_PLAYING":
                publishPlaybackState(true);
                break;
            case "STATE_PAUSED":
                publishPlaybackState(false);
                break;
            case "METADATA":
                publishMetadata(
                        intent.getStringExtra("AUTHOR"),
                        intent.getStringExtra("TITLE"));
                break;
            case "NEXT":
                sendMessageToActivity("NEXT");
                break;
            case "PREVIOUS":
                sendMessageToActivity("PREVIOUS");
                break;
            case "START":
                break;
        }
        return START_STICKY;
    }

    /** Start-only service; nothing binds to it. */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        Log.w(TAG, "svc: FGS timeout type=" + fgsType + "; re-asserting notification");
        ensureNotification();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                serviceChannel.setAllowBubbles(false);
            }
            serviceChannel.setBypassDnd(true);
            serviceChannel.setSound(null, null);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void buildNotification() {
        createNotificationChannel();
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.setAction("OPEN");
        PendingIntent mainPendingIntent = PendingIntent.getActivity(this,
                0, mainIntent, PendingIntent.FLAG_IMMUTABLE);
        Intent deleteIntent = new Intent(this, WebViewService.class);
        deleteIntent.setAction("DESTROY");
        deletePendingIntent = PendingIntent.getService(this,
                0,
                deleteIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent toggleIntent = new Intent(this, WebViewService.class);
        toggleIntent.setAction("TOGGLE");
        togglePendingIntent = PendingIntent.getService(this,
                0,
                toggleIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, WebViewService.class);
        nextIntent.setAction("NEXT");
        nextPendingIntent = PendingIntent.getService(this,
                0,
                nextIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent previousIntent = new Intent(this, WebViewService.class);
        previousIntent.setAction("PREVIOUS");
        previousPendingIntent = PendingIntent.getService(this,
                0,
                previousIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(mainPendingIntent)
                .setLargeIcon(ytmIcon)
                .setColor(Color.parseColor("#8C90C8"))
                .setDeleteIntent(deletePendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setStyle(mediaStyle)
                // SERVICE, not TRANSPORT: TRANSPORT is what One UI files under
                // Now Playing and auto-hides when the stream is paused or dies.
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setForegroundServiceBehavior(
                        NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setAllowSystemGeneratedContextualActions(false)
                .addAction(android.R.drawable.ic_media_previous, "PREV", previousPendingIntent)
                .addAction(android.R.drawable.ic_media_play, "PLAY", togglePendingIntent)
                .addAction(android.R.drawable.ic_media_next, "NEXT", nextPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", deletePendingIntent);
        applyNotificationText(builder);
    }

    private void ensureNotification() {
        mediaSession.setActive(true);
        if (builder == null) {
            buildNotification();
            if (isPlaying != null) {
                boolean playing = isPlaying;
                isPlaying = null;
                publishPlaybackState(playing);
                return;
            }
        }
        postNotification();
    }

    private void destroyService() {
        mediaSession.setActive(false);
        mediaSession.release();
        sendMessageToActivity("DESTROY");
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
            manager.cancelAll();
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    /**
     * Publishes what the audio is really doing. A stale session state makes the
     * system send corrective transport commands and shows the wrong button.
     */
    private void publishPlaybackState(boolean playing) {
        if (isPlaying != null && isPlaying == playing) {
            postNotification();
            return;
        }
        isPlaying = playing;
        Log.d(TAG, "svc: session state -> " + (playing ? "PLAYING" : "PAUSED"));
        mediaSession.setActive(true);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(playing ? PlaybackStateCompat.STATE_PLAYING
                        : PlaybackStateCompat.STATE_PAUSED, 0, playing ? 1f : 0f)
                .build());
        if (builder == null) {
            return;
        }
        builder.clearActions()
                .addAction(android.R.drawable.ic_media_previous, "PREV", previousPendingIntent)
                .addAction(playing ? android.R.drawable.ic_media_pause
                                : android.R.drawable.ic_media_play,
                        playing ? "PAUSE" : "PLAY", togglePendingIntent)
                .addAction(android.R.drawable.ic_media_next, "NEXT", nextPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", deletePendingIntent);
        applyNotificationText(builder);
        postNotification();
    }

    private void publishMetadata(String newAuthor, String newTitle) {
        author = newAuthor != null ? newAuthor : "";
        title = newTitle != null ? newTitle : "";
        mediaSession.setActive(true);
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, ytmIcon)
                .putBitmap(MediaMetadata.METADATA_KEY_ART, ytmIcon)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, -1L)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, displayArtist())
                .putString(MediaMetadata.METADATA_KEY_TITLE, displayTitle())
                .putString(MediaMetadata.METADATA_KEY_ALBUM, "monochrome")
                .build());
        if (builder == null) {
            return;
        }
        applyNotificationText(builder);
        postNotification();
    }

    private String displayTitle() {
        return title.isEmpty() ? "monochrome" : title;
    }

    private String displayArtist() {
        return author.isEmpty() ? "playback" : author;
    }

    private void applyNotificationText(NotificationCompat.Builder target) {
        String shownTitle = displayTitle();
        String shownArtist = displayArtist();
        target.setContentTitle(shownTitle)
                .setContentText(shownArtist)
                .setSubText(shownTitle)
                .setOngoing(true);
    }

    private void postNotification() {
        if (builder == null) {
            return;
        }
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR
                | Notification.FLAG_ONGOING_EVENT
                | Notification.FLAG_FOREGROUND_SERVICE;
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
    }

    private void sendMessageToActivity(String action) {
        Log.d(TAG, "svc: -> activity " + action);
        Intent intent = new Intent();
        intent.setAction(RECEIVER);
        // Android 14+ drops an implicit broadcast to a non-exported receiver, so
        // scope it to this package or the notification buttons do nothing.
        intent.setPackage(getPackageName());
        intent.putExtra("ACTION", action);
        sendBroadcast(intent);
    }

    MediaSessionCompat.Callback callback = new MediaSessionCompat.Callback() {
        // PLAY and PAUSE rather than TOGGLE: the system can deliver these more than
        // once for a single user action, and a blind flip turns the repeat into a
        // pause right after the play.
        @Override
        public void onPlay() {
            Log.d(TAG, "svc: media session onPlay()");
            sendMessageToActivity("PLAY");
        }

        @Override
        public void onPause() {
            Log.d(TAG, "svc: media session onPause()");
            sendMessageToActivity("PAUSE");
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "svc: media session onSkipToPrevious()");
            sendMessageToActivity("PREVIOUS");
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "svc: media session onSkipToNext()");
            sendMessageToActivity("NEXT");
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
            String intentAction = mediaButtonIntent.getAction();
            if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
                KeyEvent event = IntentCompat.getParcelableExtra(
                        mediaButtonIntent, Intent.EXTRA_KEY_EVENT, KeyEvent.class);

                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                    int keycode = event.getKeyCode();
                    Log.d(TAG, "svc: media key " + KeyEvent.keyCodeToString(keycode));

                    if (keycode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                        sendMessageToActivity("NEXT");
                    }
                    if (keycode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                        sendMessageToActivity("PREVIOUS");
                    }
                    if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                        sendMessageToActivity("PLAY");
                    }
                    if (keycode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                        sendMessageToActivity("PAUSE");
                    }
                    if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                            || keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
                        sendMessageToActivity("TOGGLE");
                    }
                }
            }
            return true;
        }
    };
}
