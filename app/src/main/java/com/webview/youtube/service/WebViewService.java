package com.webview.youtube.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import com.webview.youtube.R;

public class WebViewService extends Service {

    private static final String ID = "foregroundServiceYT";                        // The id of the notification

    private PowerManager.WakeLock wakeLock;                 // PARTIAL_WAKELOCK

    /**
     * Returns the instance of the service
     */
    public class LocalBinder extends Binder {
        public WebViewService getServiceInstance() {
            return WebViewService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();      // IBinder

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YT:wakelock");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        if (action != null) {
            if (action.equals("DESTROY")) {
                destroyService();
            } else if (action.equals("START")) {
                createNotificationChannel();
                Intent notificationIntent = new Intent(this, WebViewService.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this,
                        0, notificationIntent, 0);

                Intent deleteIntent = new Intent(this, WebViewService.class);
                deleteIntent.setAction("DESTROY");
                PendingIntent deletePendingIntent = PendingIntent.getService(this,
                        0,
                        deleteIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                Notification notification = new NotificationCompat.Builder(this, ID)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setSmallIcon(android.R.color.transparent)
                        .setContentIntent(pendingIntent)
                        .setDeleteIntent(deletePendingIntent)
                        .setAutoCancel(true)
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", deletePendingIntent)
                        .build();
                startForeground(1, notification);
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
                    ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            serviceChannel.setSound(null, null);
            serviceChannel.setImportance(NotificationManager.IMPORTANCE_MIN);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void destroyService() {
        stopForeground(true);
        stopSelf();
        System.exit(0);
    }

}
