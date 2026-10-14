package com.example.internetspeed;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.net.TrafficStats;

import java.util.Locale;

public class SpeedService extends Service {
    private static final String CHANNEL = "internet_speed";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long previousBytes = -1;
    private long previousTime = 0;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            long bytes = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();
            if (bytes >= 0) {
                if (previousBytes >= 0 && now > previousTime) {
                    double bytesPerSecond = (bytes - previousBytes) * 1000.0 / (now - previousTime);
                    String value = format(bytesPerSecond);
                    getSharedPreferences("speed", MODE_PRIVATE).edit()
                        .putString("value", value).putBoolean("running", true).apply();
                    NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                    nm.notify(1, buildNotification(value));
                }
                previousBytes = bytes;
                previousTime = now;
            } else {
                getSharedPreferences("speed", MODE_PRIVATE).edit()
                    .putString("value", "Indisponible").putBoolean("running", true).apply();
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(1, buildNotification("Mesure en cours…"));
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        getSharedPreferences("speed", MODE_PRIVATE).edit().putBoolean("running", true).apply();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        return START_STICKY;
    }
    private String format(double bps) {
        if (bps < 1024) return String.format(Locale.FRANCE, "%.0f o/s", bps);
        if (bps < 1024 * 1024) return String.format(Locale.FRANCE, "%.1f Ko/s", bps / 1024);
        return String.format(Locale.FRANCE, "%.2f Mo/s", bps / (1024 * 1024));
    }
    private Notification buildNotification(String value) {
        if (Build.VERSION.SDK_INT >= 26) {
            return new Notification.Builder(this, CHANNEL)
                .setContentTitle("Vitesse Internet")
                .setContentText(value)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(true).build();
        }
        return new Notification.Builder(this)
            .setContentTitle("Vitesse Internet").setContentText(value)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(true).build();
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Vitesse Internet",
                NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }
    @Override public void onDestroy() {
        handler.removeCallbacks(ticker);
        getSharedPreferences("speed", MODE_PRIVATE).edit().putBoolean("running", false).apply();
        stopForeground(true);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
