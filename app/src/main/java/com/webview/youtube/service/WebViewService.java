package com.webview.youtube.service;

import static com.webview.youtube.MainActivity.RECEIVER;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
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

import android.content.pm.ServiceInfo;

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
    private androidx.media.app.NotificationCompat.MediaStyle mediaStyle;
    private MediaSessionCompat mediaSession;
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

        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        playbackStateBuilder
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0f);
        ytIcon = BitmapFactory.decodeResource(getResources(), R.drawable.youtube);

        manager = getSystemService(NotificationManager.class);
        mediaSession = new MediaSessionCompat(getApplicationContext(), "YT:mediaService");
        mediaStyle = new androidx.media.app.NotificationCompat.MediaStyle();
        mediaSession.setPlaybackState(playbackStateBuilder.build());
        mediaSession.setCallback(callback);
        mediaSession.setActive(true);
        mediaStyle.setShowActionsInCompactView(0, 1, 2);
        mediaStyle.setShowCancelButton(true);
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, ytIcon)
                .putBitmap(MediaMetadata.METADATA_KEY_ART, ytIcon)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, -1L)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "playback")
                .putString(MediaMetadata.METADATA_KEY_TITLE, "YouTube")
                .putString(MediaMetadata.METADATA_KEY_ALBUM, "YouTube")
                .build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == intent) {
            return START_STICKY;
        }
        final String action = intent.getAction();
        Log.d(TAG, "svc: command " + action + " (startId=" + startId + ")");
        if (action != null) {
            mediaStyle.setMediaSession(mediaSession.getSessionToken());
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
                            .setStyle(mediaStyle)
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
        mediaSession.setActive(true);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                .setState(playing ? PlaybackStateCompat.STATE_PLAYING
                        : PlaybackStateCompat.STATE_PAUSED, 0, playing ? 1f : 0f)
                .build());
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
            sendMessageToActivity("TOGGLE");
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

                    if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                        sendMessageToActivity("PLAY");
                    }

                    if (keycode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                        sendMessageToActivity("PAUSE");
                    }

                    if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                            || keycode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                            || keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
                        sendMessageToActivity("TOGGLE");
                    }
                }
            }
            return true;
        }
    };
}
