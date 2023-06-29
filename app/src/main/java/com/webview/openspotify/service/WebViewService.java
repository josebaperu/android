package com.webview.openspotify.service;

import static com.webview.openspotify.MainActivity.RECEIVER;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import com.webview.openspotify.MainActivity;

public class WebViewService extends Service {

    private static final String CHANNEL_ID = "foregroundServiceOpenSpotify";                        // The id of the notification

    private PowerManager.WakeLock wakeLock;                 // PARTIAL_WAKELOCK

    private NotificationManager manager;

    /**
     * Returns the instance of the service
     */
    public class LocalBinder extends Binder {
        public WebViewService getServiceInstance() {
            return WebViewService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();      // IBinder
    private NotificationCompat.Builder builder;

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OS:wakelock");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String playing = "";
        if(null == intent) {
            return START_STICKY;
        }
        final String action = intent.getAction();
        if (action != null) {
            if (action.equals("PLAYING") && null != builder) {
                Bundle b = intent.getExtras();
                String msg = b.getString("PLAYING");
                android.util.Log.d("SERVICE", msg);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    manager = getSystemService(NotificationManager.class);
                    builder.setContentText(msg);
                    manager.notify(1, builder.build());
                }
            }
            if (action.equals("DESTROY")) {
                destroyService();
            } else if (action.equals("START")) {
                createNotificationChannel();
                Intent notificationIntent = new Intent(this, WebViewService.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this,
                        0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                Intent mainIntent = new Intent (this, MainActivity.class);
                PendingIntent mainPendingIntent = PendingIntent.getActivity(this,
                        0, mainIntent, 0);
                Intent deleteIntent = new Intent(this, WebViewService.class);
                deleteIntent.setAction("DESTROY");
                PendingIntent deletePendingIntent = PendingIntent.getService(this,
                        0,
                        deleteIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentText(playing)
                        .setSmallIcon(android.R.color.transparent)
                        .setContentIntent(pendingIntent)
                        .setDeleteIntent(deletePendingIntent)
                        .setAutoCancel(true)
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", deletePendingIntent)
                        .addAction(android.R.drawable.button_onoff_indicator_on, "OPEN SPOTIFY", mainPendingIntent);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    manager = getSystemService(NotificationManager.class);
                    manager.notify(1, builder.build());
                }
                startForeground(1, builder.build());
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
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager = getSystemService(NotificationManager.class);
            serviceChannel.setSound(null, null);
            serviceChannel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void destroyService() {
        sendMessageToActivity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager = getSystemService(NotificationManager.class);
            manager.cancel(1);
            manager.cancelAll();
        }
        stopForeground(true);
        stopSelf();
        onDestroy();
    }
    private void sendMessageToActivity() {
        Intent intent = new Intent(RECEIVER);
        sendBroadcast(intent);
    }

}
