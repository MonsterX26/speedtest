package com.monsterx26.speedtest;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SpeedService extends Service {

    private static final String CHANNEL = "internet_usage";
    private static final int NOTIFICATION_ID = 1;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private long previousBytes = -1;
    private long previousTime = 0;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();

            long rx = TrafficStats.getTotalRxBytes();
            long tx = TrafficStats.getTotalTxBytes();

            long total = (rx < 0 || tx < 0)
                    ? -1 : rx + tx;

            String speed = "Indisponible";

            if (total >= 0) {
                if (previousBytes >= 0
                        && total >= previousBytes
                        && now > previousTime) {

                    double rate = (total - previousBytes)
                            * 1000.0 / (now - previousTime);

                    speed = formatRate(rate);
                } else {
                    speed = "0 KB/s";
                }

                previousBytes = total;
                previousTime = now;
                updateUsage(total, now);
            }

            android.content.SharedPreferences prefs =
                    getSharedPreferences("speed", MODE_PRIVATE);

            prefs.edit()
                    .putString("value", speed)
                    .putBoolean("running", true)
                    .apply();

            String daily = prefs.getString(
                    "daily", "Aujourd’hui : calcul…");

            String monthly = prefs.getString(
                    "monthly", "Ce mois : calcul…");

            NotificationManager nm =
                    (NotificationManager)
                            getSystemService(NOTIFICATION_SERVICE);

            nm.notify(NOTIFICATION_ID,
                    buildNotification(speed, daily, monthly));

            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();

        startForeground(NOTIFICATION_ID,
                buildNotification("Mesure…",
                        "Aujourd’hui : calcul…",
                        "Ce mois : calcul…"));
    }

    @Override
    public int onStartCommand(Intent intent,
                              int flags, int startId) {
        handler.removeCallbacks(ticker);
        previousBytes = -1;
        previousTime = 0;
        handler.post(ticker);

        getSharedPreferences("speed", MODE_PRIVATE)
                .edit()
                .putBoolean("running", true)
                .apply();

        return START_STICKY;
    }

    private String formatRate(double bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return String.format(Locale.US,
                    "%.0f B/s", bytesPerSecond);
        }

        if (bytesPerSecond < 1024 * 1024) {
            return String.format(Locale.US,
                    "%.1f KB/s", bytesPerSecond / 1024.0);
        }

        return String.format(Locale.US,
                "%.2f MB/s",
                bytesPerSecond / (1024.0 * 1024));
    }

    private void updateUsage(long totalBytes, long now) {
        android.content.SharedPreferences p =
                getSharedPreferences("usage", MODE_PRIVATE);

        String day = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.US)
                .format(new Date(now));

        String month = new SimpleDateFormat(
                "yyyy-MM", Locale.US)
                .format(new Date(now));

        String oldDay = p.getString("day", "");
        String oldMonth = p.getString("month", "");

        long lastTotal = p.getLong("lastTotal", totalBytes);
        long daily = p.getLong("daily", 0);
        long monthly = p.getLong("monthly", 0);

        if (!day.equals(oldDay)) daily = 0;
        if (!month.equals(oldMonth)) monthly = 0;

        long delta = totalBytes >= lastTotal
                ? totalBytes - lastTotal : 0;

        daily += delta;
        monthly += delta;

        p.edit()
                .putString("day", day)
                .putString("month", month)
                .putLong("lastTotal", totalBytes)
                .putLong("daily", daily)
                .putLong("monthly", monthly)
                .apply();

        getSharedPreferences("speed", MODE_PRIVATE)
                .edit()
                .putString("daily",
                        "Aujourd’hui : " + formatBytes(daily))
                .putString("monthly",
                        "Ce mois : " + formatBytes(monthly))
                .apply();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return String.format(Locale.US,
                    "%.0f B", (double) bytes);
        }

        if (bytes < 1024L * 1024) {
            return String.format(Locale.US,
                    "%.1f KB", bytes / 1024.0);
        }

        if (bytes < 1024L * 1024 * 1024) {
            return String.format(Locale.US,
                    "%.2f MB", bytes / (1024.0 * 1024));
        }

        return String.format(Locale.US,
                "%.2f GB",
                bytes / (1024.0 * 1024 * 1024));
    }

    private Notification buildNotification(
            String speed, String daily, String monthly) {

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(this, CHANNEL);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("Vitesse Internet : " + speed)
                .setContentText(daily + " • " + monthly)
                .setStyle(new Notification.BigTextStyle()
                        .bigText("Vitesse actuelle : " + speed
                                + "\n" + daily
                                + "\n" + monthly))
                .setSmallIcon(
                        android.R.drawable.stat_notify_sync_noanim)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false);

        return builder.build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL,
                            "Vitesse Internet",
                            NotificationManager.IMPORTANCE_LOW);

            channel.setDescription(
                    "Suivi de la vitesse du réseau.");
            channel.setShowBadge(false);

            NotificationManager nm =
                    (NotificationManager)
                            getSystemService(NOTIFICATION_SERVICE);

            nm.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(ticker);

        getSharedPreferences("speed", MODE_PRIVATE)
                .edit()
                .putBoolean("running", false)
                .apply();

        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}