
package com.monsterx26.speedtest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.net.TrafficStats;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SpeedService extends Service {

    private static final String CHANNEL = "speed_lite_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final long INTERVAL = 500L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private long previousRx = -1;
    private long previousTx = -1;
    private long previousTime = 0;

    private SharedPreferences prefs;
    private NotificationManager notificationManager;

    private final DecimalFormat decimal =
            new DecimalFormat("0.0");

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateSpeed();
            handler.postDelayed(this, INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = getSharedPreferences("SpeedPrefs", MODE_PRIVATE);
        notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        createNotificationChannel();

        Notification notification =
                buildNotification("0 KB/s", "0 KB/s");

        startForeground(NOTIFICATION_ID, notification);

        previousRx = TrafficStats.getTotalRxBytes();
        previousTx = TrafficStats.getTotalTxBytes();
        previousTime = System.currentTimeMillis();

        handler.post(ticker);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void updateSpeed() {
        long rx = TrafficStats.getTotalRxBytes();
        long tx = TrafficStats.getTotalTxBytes();
        long now = System.currentTimeMillis();

        if (rx < 0 || tx < 0) {
            return;
        }

        if (previousTime == 0) {
            previousRx = rx;
            previousTx = tx;
            previousTime = now;
            return;
        }

        long elapsed = now - previousTime;

        if (elapsed <= 0) {
            return;
        }

        double seconds = elapsed / 1000.0;

        double download = Math.max(0, rx - previousRx) / seconds;
        double upload = Math.max(0, tx - previousTx) / seconds;

        previousRx = rx;
        previousTx = tx;
        previousTime = now;

        String downloadText = formatSpeed(download);
        String uploadText = formatSpeed(upload);

        saveUsage(rx, tx);

        Notification notification =
                buildNotification(downloadText, uploadText);

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private String formatSpeed(double bytesPerSecond) {
        if (bytesPerSecond >= 1024 * 1024) {
            return decimal.format(bytesPerSecond / (1024 * 1024))
                    + " MB/s";
        }

        if (bytesPerSecond >= 1024) {
            return decimal.format(bytesPerSecond / 1024)
                    + " KB/s";
        }

        return Math.round(bytesPerSecond) + " B/s";
    }

    private Bitmap createSpeedIcon(String speed) {
        int width = 96;
        int height = 96;

        Bitmap bitmap = Bitmap.createBitmap(
                width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(
                "sans-serif-condensed", Typeface.BOLD));

        String number;
        String unit;

        String[] parts = speed.split(" ");

        if (parts.length >= 2) {
            number = parts[0];
            unit = parts[1];
        } else {
            number = speed;
            unit = "";
        }

        paint.setTextSize(number.length() >= 5 ? 29 : 36);
        canvas.drawText(number, width / 2f, 42, paint);

        paint.setTextSize(20);
        canvas.drawText(unit, width / 2f, 72, paint);

        return bitmap;
    }

    private Notification buildNotification(
            String download, String upload) {

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
                .setContentTitle("Speed Lite+")
                .setContentText("↓ " + download + "   ↑ " + upload)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL,
                    "Vitesse Internet",
                    NotificationManager.IMPORTANCE_LOW);

            channel.setDescription(
                    "Affichage du débit Internet en temps réel");
            channel.setShowBadge(false);

            notificationManager.createNotificationChannel(channel);
        }
    }

    private void saveUsage(long rx, long tx) {
        String today = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.US).format(new Date());

        String month = new SimpleDateFormat(
                "yyyy-MM", Locale.US).format(new Date());

        String savedDay = prefs.getString("day", "");
        String savedMonth = prefs.getString("month", "");

        long daily = prefs.getLong("daily", 0);
        long monthly = prefs.getLong("monthly", 0);

        if (!today.equals(savedDay)) {
            daily = 0;
        }

        if (!month.equals(savedMonth)) {
            monthly = 0;
        }

        long lastRx = prefs.getLong("lastRx", rx);
        long lastTx = prefs.getLong("lastTx", tx);

        long deltaRx = Math.max(0, rx - lastRx);
        long deltaTx = Math.max(0, tx - lastTx);
        long delta = deltaRx + deltaTx;

        daily += delta;
        monthly += delta;

        prefs.edit()
                .putString("day", today)
                .putString("month", month)
                .putLong("daily", daily)
                .putLong("monthly", monthly)
                .putLong("lastRx", rx)
                .putLong("lastTx", tx)
                .apply();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
