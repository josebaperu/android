package com.webview.youtube.service;

import static com.webview.youtube.MainActivity.RECEIVER;

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
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.IntentCompat;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaStyleNotificationHelper;

import com.webview.youtube.MainActivity;
import com.webview.youtube.R;

public class WebViewService extends Service {

    /** Watch the whole transport chain with: adb logcat -s YT:D */
    private static final String TAG = "YT";

    private static final String APP_NAME = "YOUTUBE";
    private static final String CHANNEL_ID = APP_NAME + "_CHANNEL_ID";
    private static final String CHANNEL_NAME = APP_NAME + "_CHANNEL_NAME";
    private static final int NOTIFICATION_ID = 1;
    private NotificationManager manager;
    private MediaSession mediaSession;
    private WebViewPlayer player;
    private Bitmap ytIcon;

    private NotificationCompat.Builder builder;
    private PendingIntent deletePendingIntent;
    private PendingIntent togglePendingIntent;
    private PendingIntent nextPendingIntent;
    /** Real state of the page's video, reported by the activity's playback bridge.
     *  Null until the first report, so an opening "paused" is still published. */
    private Boolean isPlaying;

    @Override
    public void onCreate() {
        super.onCreate();

        ytIcon = BitmapFactory.decodeResource(getResources(), R.drawable.youtube);
        manager = getSystemService(NotificationManager.class);

        player = new WebViewPlayer(Looper.getMainLooper(), new WebViewPlayer.Callback() {
            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady) {
                sendMessageToActivity(playWhenReady ? "PLAY" : "PAUSE");
            }

            @Override
            public void onSkipToNext() {
                sendMessageToActivity("NEXT");
            }
        });
        mediaSession = new MediaSession.Builder(this, player)
                .setId("YT:mediaService")
                .setCallback(sessionCallback)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == intent) {
            return START_STICKY;
        }
        final String action = intent.getAction();
        Log.d(TAG, "svc: command " + action + " (startId=" + startId + ")");
        if (action != null) {
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
                case "NEXT":
                    sendMessageToActivity("NEXT");
                    break;
                case "DESTROY":
                    destroyService();
                    break;
                case "START":
                    if (mediaSession == null) {
                        break;
                    }
                    createNotificationChannel();

                    Intent mainIntent = new Intent(this, MainActivity.class);
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


                    builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                            .setContentIntent(mainPendingIntent)
                            .setDeleteIntent(deletePendingIntent)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setLargeIcon(ytIcon)
                            .setColor(Color.parseColor("#E77200"))
                            .setOngoing(true)
                            .setBadgeIconType(androidx.core.app.NotificationCompat.BADGE_ICON_NONE)
                            .setOnlyAlertOnce(true)
                            .setStyle(new MediaStyleNotificationHelper.MediaStyle(mediaSession)
                                    .setShowActionsInCompactView(0, 1, 2))
                            .setAllowSystemGeneratedContextualActions(true)
                            .setCategory(Notification.CATEGORY_TRANSPORT)
                            .setForegroundServiceBehavior(
                                    NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", deletePendingIntent)
                            .addAction(android.R.drawable.ic_media_play, "PLAY", togglePendingIntent)
                            .addAction(android.R.drawable.ic_media_next, "NEXT", nextPendingIntent);
                    applyNotificationText(builder);
                    postNotification();
                    if (isPlaying != null) {
                        boolean playing = isPlaying;
                        isPlaying = null;
                        publishPlaybackState(playing);
                    } else {
                        publishPlaybackState(false);
                    }
                    break;
            }
        }
        return START_STICKY;
    }

    /** Start-only service; nothing binds to it. */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setSound(null, null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                serviceChannel.setAllowBubbles(false);
            }
            serviceChannel.setBypassDnd(true);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void destroyService() {
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        sendMessageToActivity("DESTROY");
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
            manager.cancelAll();
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    /**
     * Publishes what the video is really doing. A stale session state makes the
     * system send corrective transport commands and shows the wrong button.
     */
    private void publishPlaybackState(boolean playing) {
        if (isPlaying != null && isPlaying == playing) {
            return;
        }
        isPlaying = playing;
        // No transport command logged just before this means the page changed state
        // on its own - that is the signature of the player pausing itself.
        Log.d(TAG, "svc: session state -> " + (playing ? "PLAYING" : "PAUSED"));
        if (player != null) {
            player.setReportedPlaying(playing);
        }
        if (builder == null) {
            return;
        }
        builder.clearActions()
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", deletePendingIntent)
                .addAction(playing ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play,
                        playing ? "PAUSE" : "PLAY", togglePendingIntent)
                .addAction(android.R.drawable.ic_media_next, "NEXT", nextPendingIntent);
        applyNotificationText(builder);
        postNotification();
    }

    private void applyNotificationText(NotificationCompat.Builder target) {
        target.setContentTitle("YouTube")
                .setContentText("playback")
                .setSubText("YouTube")
                .setOngoing(true);
    }

    /** Samsung drops a paused MediaStyle notify() when audio dies; re-assert FGS. */
    private void postNotification() {
        if (builder == null) {
            return;
        }
        Notification notification = builder.build();
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
    }

    private void sendMessageToActivity(String action) {
        Log.d(TAG, "svc: -> activity " + action);
        Intent intent = new Intent();
        intent.setAction(RECEIVER);
        // Android 14+ refuses to deliver an implicit broadcast to a non-exported
        // receiver, so scope it to this package or the notification buttons do nothing.
        intent.setPackage(getPackageName());
        intent.putExtra("ACTION", action);
        sendBroadcast(intent);
    }

    private final MediaSession.Callback sessionCallback = new MediaSession.Callback() {
        @Override
        public boolean onMediaButtonEvent(MediaSession session, MediaSession.ControllerInfo controllerInfo,
                                          Intent mediaButtonIntent) {
            if (!Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonIntent.getAction())) {
                return MediaSession.Callback.super.onMediaButtonEvent(
                        session, controllerInfo, mediaButtonIntent);
            }
            KeyEvent event = IntentCompat.getParcelableExtra(
                    mediaButtonIntent, Intent.EXTRA_KEY_EVENT, KeyEvent.class);
            if (event == null || event.getAction() != KeyEvent.ACTION_DOWN) {
                return MediaSession.Callback.super.onMediaButtonEvent(
                        session, controllerInfo, mediaButtonIntent);
            }
            int keycode = event.getKeyCode();
            Log.d(TAG, "svc: media key " + KeyEvent.keyCodeToString(keycode));
            // Previous/headset-hook stay as TOGGLE: they are not advertised as session
            // actions, so the default Player mapping would drop them.
            if (keycode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                    || keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
                sendMessageToActivity("TOGGLE");
                return true;
            }
            return MediaSession.Callback.super.onMediaButtonEvent(
                    session, controllerInfo, mediaButtonIntent);
        }
    };
}
