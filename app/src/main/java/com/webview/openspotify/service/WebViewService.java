package com.webview.openspotify.service;

import static com.webview.openspotify.MainActivity.RECEIVER;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import androidx.core.app.NotificationCompat;

import com.webview.openspotify.MainActivity;
import com.webview.openspotify.R;

public class WebViewService extends Service {

    private final String APP_NAME = "OPEN_SPOTIFY";
    private final String CHANNEL_ID = APP_NAME.concat("_CHANNEL_ID");
    private final String CHANNEL_NAME = APP_NAME.concat("_CHANNEL_NAME");
    private PowerManager.WakeLock wakeLock;                 // PARTIAL_WAKELOCK
    private NotificationManager manager;
    private androidx.media.app.NotificationCompat.MediaStyle mediaStyle;
    private MediaSessionCompat mediaSession;
    /**
     * Returns the instance of the service
     */
    public static class LocalBinder extends Binder {
    }

    private final IBinder mBinder = new LocalBinder();      // IBinder
    private NotificationCompat.Builder builder;

    private Bitmap spotifyIcon;
    private String title ="";
    private String author ="";

    @Override
    public void onCreate() {
        super.onCreate();


        spotifyIcon = BitmapFactory.decodeResource(getResources(), R.drawable.icons8_spotify_48);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OS:wakelock");
        mediaSession = new MediaSessionCompat(getApplicationContext(), "OS:mediaService");
        mediaStyle = new androidx.media.app.NotificationCompat.MediaStyle();

        mediaSession.setCallback(callback);
        mediaSession.setActive(true);
        mediaStyle.setShowActionsInCompactView(0, 1, 2);
        mediaStyle.setShowCancelButton(true);
    }
    private String getValue() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString("PLAYING", "");
    }

    private void save(String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString("PLAYING", value);
        editor.apply();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == intent) {
            return START_STICKY;
        }
        final String action = intent.getAction();
        createNotificationChannel();
        if (action != null) {

            mediaStyle.setMediaSession(mediaSession.getSessionToken());
            if (action.equals("PLAYING") && null != builder) {
                Bundle b = intent.getExtras();
                String msg = b != null && b.containsKey("PLAYING") ? b.getString("PLAYING") : "";
                String value = getValue();
                if (msg != null && !msg.equals(value)) {
                    save(msg);
                    String[] metadataArray = msg.split("-");
                    boolean isValidMetadata = metadataArray.length > 0;
                    author = isValidMetadata ? metadataArray[0] : "";
                    title = isValidMetadata ? metadataArray[1] : "";
                    mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, spotifyIcon)
                            .putBitmap(MediaMetadata.METADATA_KEY_ART, spotifyIcon)
                            .putLong(MediaMetadata.METADATA_KEY_DURATION, -1L)
                            .putString(MediaMetadata.METADATA_KEY_ARTIST, author)
                            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                            .putString(MediaMetadata.METADATA_KEY_ALBUM, "spotify")
                            .build());
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        manager = getSystemService(NotificationManager.class);
                        builder.setContentText(author);
                        builder.setContentTitle(title);
                        builder.setSubText(title);
                        manager.notify(1, builder.build());
                    }
                    mediaStyle.setMediaSession(mediaSession.getSessionToken());
                    PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
                    long stateActions = PlaybackStateCompat.ACTION_PLAY
                            | PlaybackStateCompat.ACTION_PLAY_PAUSE
                            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
                    playbackStateBuilder.setActions(stateActions);
                    mediaSession.setPlaybackState(playbackStateBuilder
                            .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
                            .build());
                    android.util.Log.d("SERVICE", msg);
                }
                //mediaSession.getController().getMetadata().getDescription()
            }
            switch (action) {
                case "NEXT":
                    sendMessageToActivity("NEXT");
                    break;
                case "TOGGLE":
                    sendMessageToActivity("TOGGLE");
                    break;
                case "DESTROY":
                    save("");
                    destroyService();
                    break;
                case "START":
                    Intent mainIntent = new Intent(this, MainActivity.class);
                    mainIntent.setAction("OPEN");
                    PendingIntent mainPendingIntent = PendingIntent.getActivity(this,
                            0, mainIntent, PendingIntent.FLAG_IMMUTABLE);
                    Intent deleteIntent = new Intent(this, WebViewService.class);
                    deleteIntent.setAction("DESTROY");
                    PendingIntent deletePendingIntent = PendingIntent.getService(this,
                            0,
                            deleteIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    Intent toggleIntent = new Intent(this, WebViewService.class);
                    toggleIntent.setAction("TOGGLE");
                    PendingIntent togglePendingIntent = PendingIntent.getService(this,
                            0,
                            toggleIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    Intent nextIntent = new Intent(this, WebViewService.class);
                    nextIntent.setAction("NEXT");
                    PendingIntent nextPendingIntent = PendingIntent.getService(this,
                            0,
                            nextIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);


                    builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setSmallIcon(android.R.color.transparent)
                            .setContentIntent(mainPendingIntent)
                            .setDeleteIntent(deletePendingIntent)
                            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                            .setOngoing(true)
                            .setBadgeIconType(androidx.core.app.NotificationCompat.BADGE_ICON_NONE)
                            .setOnlyAlertOnce(true)
                            .setStyle(mediaStyle)
                            .setCategory(Notification.CATEGORY_SERVICE)
                            .setColorized(true)
                            .setAutoCancel(false)
                            .setContentTitle(title)
                            .setContentText(author)
                            .setSubText(title)
                            .setAllowSystemGeneratedContextualActions(true)
                            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", deletePendingIntent)
                            .addAction(android.R.drawable.ic_media_play, "AUDIO", togglePendingIntent)
                            .addAction(android.R.drawable.ic_media_next, "NEXT", nextPendingIntent);

                    startForeground(1, builder.build());
                    break;
            }
        }
        return START_STICKY;
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public IBinder onBind(Intent intent) {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
        return mBinder;
    }

    @Override
    public void onDestroy() {
        // PARTIAL_WAKELOCK
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
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
            manager = getSystemService(NotificationManager.class);
            serviceChannel.setSound(null, null);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void destroyService() {
        mediaSession.setActive(false);
        mediaSession.release();
        sendMessageToActivity("DESTROY");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.cancel(1);
            manager.cancelAll();
        }
        stopForeground(true);
        stopSelf();
        onDestroy();
    }

    private void sendMessageToActivity(String action) {
        Intent intent = new Intent();
        intent.setAction(RECEIVER);
        intent.putExtra("ACTION", action);
        sendBroadcast(intent);
    }

    MediaSessionCompat.Callback callback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            sendMessageToActivity("TOGGLE");
        }

        @Override
        public void onPause() {
            sendMessageToActivity("TOGGLE");
        }

        @Override
        public void onSkipToPrevious() {
            sendMessageToActivity("TOGGLE");
        }

        @Override
        public void onSkipToNext() {
            sendMessageToActivity("NEXT");
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
            String intentAction = mediaButtonIntent.getAction();
            if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
                KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                    int keycode = event.getKeyCode();

                    if (keycode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                        sendMessageToActivity("NEXT");
                    }

                    if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keycode == KeyEvent.KEYCODE_MEDIA_PLAY || keycode == KeyEvent.KEYCODE_MEDIA_PAUSE || keycode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
                        sendMessageToActivity("TOGGLE");
                    }
                }
            }
            return true;
        }
    };

}
