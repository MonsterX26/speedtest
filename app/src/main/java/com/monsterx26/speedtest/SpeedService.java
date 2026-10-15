package com.monsterx26.speedtest;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
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

            long total = (rx < 0 || tx < 0) ? -1 : rx + tx;

            String speed = "Indisponible";

            if (total >= 0) {
                if (previousBytes >= 0
                        && total >= previousBytes
                        && now > previousTime) {

                    double rate =
                            (total - previousBytes) * 1000.0
                            / (now - previousTime);

                    speed = formatRate(rate);
                } else {
                    speed = "0 KB/s";
                }

                previousBytes = total;
                previousTime = now;

                updateUsage(total, now);
            }

            getSharedPreferences("speed", MODE_PRIVATE)
                    .edit()
                    .putString("value", speed)
                    .putBoolean("running", true)
                    .apply();

            NotificationManager nm =
                    (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);

            nm.notify(
                    1,
                    buildNotification(
                            speed,
                            getSharedPreferences("speed", MODE_PRIVATE)
                                    .getString(
                                            "daily",
                                            "Aujourd'hui : calcul…"),
                            getSharedPreferences("speed", MODE_PRIVATE)
                                    .getString(
                                            "monthly",
                                            "Ce mois : calcul…")
                    )
            );

            handler.postDelayed(this, 500);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        createChannel();

        startForeground(
                1,
                buildNotification(
                        "Mesure…",
                        "Aujourd'hui : calcul…",
                        "Ce mois : calcul…"
                )
        );
    }

    @Override
    public int onStartCommand(
            Intent intent, int flags, int startId) {

        getSharedPreferences("speed", MODE_PRIVATE)
                .edit()
                .putBoolean("running", true)
                .apply();

        handler.removeCallbacks(ticker);
        handler.post(ticker);

        return START_STICKY;
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
                ? totalBytes - lastTotal
                : 0;

        daily += delta;
        monthly += delta;

        p.edit()
                .putString("day", day)
                .putString("month", month)
                .putLong("lastTotal", totalBytes)
                .putLong("daily", daily)
                .putLong("monthly", monthly)
                .apply();

        String dailyText =
                "Aujourd'hui : " + formatBytes(daily);

        String monthlyText =
                "Ce mois : " + formatBytes(monthly);

        getSharedPreferences("speed", MODE_PRIVATE)
                .edit()
                .putString("daily", dailyText)
                .putString("monthly", monthlyText)
                .apply();
    }

    private String formatRate(double bps) {
        if (bps < 1024) {
            return String.format(
                    Locale.US, "%.0f B/s", bps);
        }

        if (bps < 1024 * 1024) {
            return String.format(
                    Locale.US, "%.1f KB/s", bps / 1024);
        }

        return String.format(
                Locale.US, "%.2f MB/s",
                bps / (1024 * 1024));
    }

    private String[] statusRateParts(String rate) {
        try {
            String trimmed = rate.trim();

            if (trimmed.endsWith("MB/s")) {
                return new String[] {
                        trimmed.substring(
                                0, trimmed.length() - 4).trim(),
                        "MB/s"
                };
            }

            if (trimmed.endsWith("KB/s")) {
                return new String[] {
                        trimmed.substring(
                                0, trimmed.length() - 4).trim(),
                        "KB/s"
                };
            }

            if (trimmed.endsWith("B/s")) {
                return new String[] {
                        trimmed.substring(
                                0, trimmed.length() - 3).trim(),
                        "B/s"
                };
            }
        } catch (Exception ignored) { }

        return new String[] { "--", "KB/s" };
    }

    private String formatBytes(long bytes) {
        double b = Math.max(0, bytes);

        if (b < 1024) {
            return String.format(Locale.US, "%.0f B", b);
        }

        if (b < 1024L * 1024) {
            return String.format(
                    Locale.US, "%.1f KB", b / 1024);
        }

        if (b < 1024L * 1024 * 1024) {
            return String.format(
                    Locale.US, "%.2f MB",
                    b / (1024 * 1024));
        }

        return String.format(
                Locale.US, "%.2f GB",
                b / (1024.0 * 1024 * 1024));
    }

    private Notification buildNotification(
            String speed, String daily, String monthly) {

        String title = "Vitesse Internet : " + speed;
        String content = daily + " • " + monthly;

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(this, CHANNEL);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle(title)
                .setContentText(content)
                .setStyle(
                        new Notification.BigTextStyle()
                                .bigText(
                                        "Vitesse actuelle : " + speed
                                        + "\n" + daily
                                        + "\n" + monthly))
                .setSmallIcon(
                        android.R.drawable.stat_notify_sync_noanim)
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        if (Build.VERSION.SDK_INT >= 23) {
            Bitmap bitmap = makeStatusIcon(speed);

            if (bitmap != null) {
                builder.setSmallIcon(
                        Icon.createWithBitmap(bitmap));
            }
        }

        return builder.build();
    }

    private Bitmap makeStatusIcon(String rate) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(
                    144, 128, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            paint.setColor(Color.WHITE);
            paint.setTypeface(
                    Typeface.create(
                            "sans-serif-condensed",
                            Typeface.BOLD));

            paint.setTextAlign(Paint.Align.CENTER);

            String[] parts = statusRateParts(rate);

            String number = parts[0];
            String unit = parts[1];

            paint.setTextSize(
                    number.length() > 4 ? 46f : 58f);

            canvas.drawText(number, 72f, 54f, paint);

            paint.setTypeface(
                    Typeface.create(
                            "sans-serif", Typeface.BOLD));

            paint.setTextSize(30f);

            canvas.drawText(unit, 72f, 96f, paint);

            return bitmap;

        } catch (Exception ignored) {
            return null;
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL,
                            "Vitesse Internet",
                            NotificationManager.IMPORTANCE_LOW);

            channel.setDescription(
                    "Affiche la vitesse réseau actuelle "
                    + "dans la barre d'état et la notification.");

            ((NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);
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