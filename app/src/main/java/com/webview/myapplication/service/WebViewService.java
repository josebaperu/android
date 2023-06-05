package com.webview.myapplication.service;

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

import com.webview.myapplication.MainActivity;
import com.webview.myapplication.R;

public class WebViewService extends Service {

    private static final String ID = "foregroundService";                        // The id of the notification

    private NotificationCompat.Builder builder;
    private NotificationManager mNotificationManager;
    private PowerManager.WakeLock wakeLock;                 // PARTIAL_WAKELOCK

    /**
     * Returns the instance of the service
     */
    public class LocalBinder extends Binder {
        public WebViewService getServiceInstance(){
            return WebViewService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();      // IBinder

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"INSERT_YOUR_APP_NAME:wakelock");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, WebViewService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Intent deleteIntent = new Intent(this, WebViewService.class);
        PendingIntent deletePendingIntent = PendingIntent.getService(this,
                0,
                deleteIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, ID)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentTitle("TVnow Playback")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", deletePendingIntent)
                .build();
        startForeground(1, notification);
        return START_NOT_STICKY;
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
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
