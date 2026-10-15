package com.example.internetspeed;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import android.net.TrafficStats;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SpeedService extends Service {
    private static final String CHANNEL = "internet_usage";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long previousBytes = -1, previousTime = 0;
    private WindowManager windowManager;
    private TextView overlay;
    private boolean overlayAdded = false;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            long rx = TrafficStats.getTotalRxBytes();
            long tx = TrafficStats.getTotalTxBytes();
            long total = (rx < 0 || tx < 0) ? -1 : rx + tx;
            String speed = "Mesure…";
            if (total >= 0) {
                if (previousBytes >= 0 && total >= previousBytes && now > previousTime) {
                    double rate = (total - previousBytes) * 1000.0 / (now - previousTime);
                    speed = formatRate(rate);
                }
                previousBytes = total;
                previousTime = now;
                updateUsage(total, now);
                if (overlay != null) overlay.setText("  ↓↑ " + speed + "  ");
            } else {
                if (overlay != null) overlay.setText("  ↓↑ Indisponible  ");
            }
            handler.postDelayed(this, 500);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(1, buildNotification("Calcul de la consommation…", "Calcul de la consommation…"));
        showOverlay();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        getSharedPreferences("speed", MODE_PRIVATE).edit().putBoolean("running", true).apply();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        return START_STICKY;
    }

    private void updateUsage(long totalBytes, long now) {
        android.content.SharedPreferences p = getSharedPreferences("usage", MODE_PRIVATE);
        String day = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(now));
        String month = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date(now));
        String oldDay = p.getString("day", "");
        String oldMonth = p.getString("month", "");
        long lastTotal = p.getLong("lastTotal", totalBytes);
        long daily = p.getLong("daily", 0);
        long monthly = p.getLong("monthly", 0);

        if (!day.equals(oldDay)) daily = 0;
        if (!month.equals(oldMonth)) monthly = 0;

        // Accumule uniquement le trafic écoulé depuis la dernière mesure.
        // Si le téléphone a redémarré, TrafficStats peut repartir à zéro.
        long delta = (totalBytes >= lastTotal) ? totalBytes - lastTotal : 0;
        daily += delta;
        monthly += delta;

        p.edit().putString("day", day).putString("month", month)
            .putLong("lastTotal", totalBytes).putLong("daily", daily)
            .putLong("monthly", monthly).apply();

        String dailyText = "Aujourd'hui : " + formatBytes(daily);
        String monthlyText = "Ce mois : " + formatBytes(monthly);
        getSharedPreferences("speed", MODE_PRIVATE).edit()
            .putString("daily", dailyText).putString("monthly", monthlyText).apply();
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.notify(1, buildNotification(dailyText, monthlyText));
    }

    private String formatRate(double bps) {
        if (bps < 1024) return String.format(Locale.US, "%.0f B/s", bps);
        if (bps < 1024 * 1024) return String.format(Locale.US, "%.1f KB/s", bps / 1024);
        return String.format(Locale.US, "%.2f MB/s", bps / (1024 * 1024));
    }

    private String formatBytes(long bytes) {
        double b = Math.max(0, bytes);
        if (b < 1024) return String.format(Locale.US, "%.0f B", b);
        if (b < 1024L * 1024) return String.format(Locale.US, "%.1f KB", b / 1024);
        if (b < 1024L * 1024 * 1024) return String.format(Locale.US, "%.2f MB", b / (1024 * 1024));
        return String.format(Locale.US, "%.2f GB", b / (1024.0 * 1024 * 1024));
    }

    private void showOverlay() {
        if (Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(this)) return;
        windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        overlay = new TextView(this);
        overlay.setText("  ↓↑ Mesure…  ");
        overlay.setTextColor(Color.WHITE);
        overlay.setTextSize(12);
        overlay.setGravity(Gravity.CENTER);
        overlay.setPadding(6, 5, 6, 5);
        overlay.setBackgroundColor(Color.rgb(8, 35, 93));
        int type = Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 8;
        params.y = (int)(28 * getResources().getDisplayMetrics().density);
        try { windowManager.addView(overlay, params); overlayAdded = true; }
        catch (Exception ignored) { overlay = null; }
    }

    private Notification buildNotification(String daily, String monthly) {
        String content = daily + "  •  " + monthly;
        if (Build.VERSION.SDK_INT >= 26) {
            return new Notification.Builder(this, CHANNEL)
                .setContentTitle("Consommation Internet")
                .setContentText(content)
                .setStyle(new Notification.BigTextStyle().bigText(daily + "\n" + monthly))
                .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
                .setOngoing(true).build();
        }
        return new Notification.Builder(this)
            .setContentTitle("Consommation Internet").setContentText(content)
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setOngoing(true).build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL,
                "Consommation Internet", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(ticker);
        getSharedPreferences("speed", MODE_PRIVATE).edit().putBoolean("running", false).apply();
        if (overlayAdded && windowManager != null && overlay != null) {
            try { windowManager.removeView(overlay); } catch (Exception ignored) {}
        }
        stopForeground(true);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
