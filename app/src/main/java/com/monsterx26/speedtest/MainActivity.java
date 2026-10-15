package com.monsterx26.speedtest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private final ExecutorService worker =
            Executors.newSingleThreadExecutor();

    private final android.os.Handler handler =
            new android.os.Handler();

    private TextView status, ping, download, upload;
    private TextView live, daily, monthly;
    private Button testButton, startMeter, stopMeter;

    private volatile boolean testing = false;

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            android.content.SharedPreferences p =
                    getSharedPreferences("speed", MODE_PRIVATE);

            live.setText("Vitesse actuelle : "
                    + p.getString("value", "Mesure…"));

            status.setText(
                    p.getBoolean("running", false)
                            ? "Compteur actif"
                            : "Compteur arrêté"
            );

            daily.setText(p.getString(
                    "daily", "Aujourd’hui : en attente"));

            monthly.setText(p.getString(
                    "monthly", "Ce mois : en attente"));

            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(Color.rgb(16, 42, 67));
        getWindow().setNavigationBarColor(Color.rgb(16, 42, 67));

        buildUi();
    }

    private TextView text(
            String value, int size, int color, boolean bold) {

        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);

        if (bold) {
            t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        t.setGravity(Gravity.CENTER);
        t.setPadding(6, 7, 6, 7);

        return t;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(18, 16, 18, 18);
        root.setBackgroundColor(Color.rgb(245, 248, 250));

        scroll.addView(root);

        root.addView(text(
                "SPEED LITE", 23, Color.rgb(16, 42, 67), true));

        root.addView(text(
                "Test et suivi de la connexion",
                13, Color.DKGRAY, false));

        status = text("Prêt", 13, Color.GRAY, false);
        root.addView(status);

        ping = addMetric(root, "PING", "—", "ms");

        download = addMetric(
                root, "TÉLÉCHARGEMENT", "—", "Mbps");

        upload = addMetric(root, "ENVOI", "—", "Mbps");

        testButton = new Button(this);
        testButton.setText("DÉMARRER LE TEST");
        root.addView(testButton, fullWidth());
        testButton.setOnClickListener(v -> beginTest());

        root.addView(text(
                "SUIVI EN TEMPS RÉEL",
                15, Color.rgb(16, 42, 67), true));

        live = text(
                "Vitesse actuelle : Mesure…",
                18, Color.rgb(0, 137, 112), true);
        root.addView(live);

        daily = text(
                "Aujourd’hui : en attente",
                14, Color.DKGRAY, false);
        root.addView(daily);

        monthly = text(
                "Ce mois : en attente",
                14, Color.DKGRAY, false);
        root.addView(monthly);

        startMeter = new Button(this);
        startMeter.setText("ACTIVER L’AFFICHAGE DANS LA BARRE D’ÉTAT");
        root.addView(startMeter, fullWidth());
        startMeter.setOnClickListener(v -> startMeter());

        stopMeter = new Button(this);
        stopMeter.setText("ARRÊTER LE COMPTEUR");
        root.addView(stopMeter, fullWidth());

        stopMeter.setOnClickListener(v -> {
            stopService(new Intent(this, SpeedService.class));
            status.setText("Compteur arrêté");
        });

        root.addView(text(
                "La vitesse actuelle apparaît dans la barre d’état "
                + "et dans la notification persistante.",
                12, Color.GRAY, false));

        setContentView(scroll);
    }

    private LinearLayout.LayoutParams fullWidth() {
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);

        p.setMargins(0, 5, 0, 5);
        return p;
    }

    private TextView addMetric(
            LinearLayout root,
            String label,
            String value,
            String unit) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(8, 8, 8, 8);
        card.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams cp = fullWidth();
        cp.setMargins(0, 5, 0, 5);
        root.addView(card, cp);

        card.addView(text(
                label, 12, Color.rgb(75, 91, 105), true));

        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER);

        TextView number = text(
                value, 30, Color.rgb(0, 137, 112), true);

        line.addView(number);
        line.addView(text(unit, 13, Color.DKGRAY, false));
        card.addView(line);

        return number;
    }

    private void startMeter() {
        Intent i = new Intent(this, SpeedService.class);

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            startForegroundService(i);
        } else {
            startService(i);
        }

        status.setText("Affichage actif dans la barre d’état");
    }

    private void beginTest() {
        if (testing) return;

        testing = true;
        testButton.setEnabled(false);
        testButton.setText("TEST EN COURS…");

        status.setText("Mesure du ping…");
        ping.setText("…");
        download.setText("…");
        upload.setText("…");

        worker.execute(() -> {

            String p = measurePing();

            runOnUiThread(() -> {
                ping.setText(p);
                status.setText("Mesure du téléchargement…");
            });

            String d = measureDownload();

            runOnUiThread(() -> {
                download.setText(d);
                status.setText("Mesure de l’envoi…");
            });

            String u = measureUpload();

            runOnUiThread(() -> {
                upload.setText(u);
                status.setText("Test terminé");

                testButton.setEnabled(true);
                testButton.setText("RECOMMENCER LE TEST");
                testing = false;
            });
        });
    }

    private String measurePing() {
        long best = Long.MAX_VALUE;
        int ok = 0;

        for (int i = 0; i < 3; i++) {
            HttpURLConnection c = null;

            try {
                long t = System.nanoTime();

                c = (HttpURLConnection) new URL(
                        "https://speed.cloudflare.com/cdn-cgi/trace"
                ).openConnection();

                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                c.setUseCaches(false);

                c.getInputStream().close();

                best = Math.min(
                        best, (System.nanoTime() - t) / 1_000_000);

                ok++;

            } catch (Exception ignored) {
            } finally {
                if (c != null) c.disconnect();
            }
        }

        return ok == 0 ? "N/D" : Long.toString(best);
    }

    private String measureDownload() {
        HttpURLConnection c = null;

        try {
            c = (HttpURLConnection) new URL(
                    "https://speed.cloudflare.com/__down?bytes=5000000"
            ).openConnection();

            c.setConnectTimeout(10000);
            c.setReadTimeout(20000);
            c.setUseCaches(false);

            long t = System.nanoTime();
            long total = 0;
            byte[] b = new byte[8192];

            java.io.InputStream in = c.getInputStream();
            int n;

            while ((n = in.read(b)) != -1) {
                total += n;
            }

            in.close();

            double sec = (System.nanoTime() - t) / 1e9;

            if (total == 0 || sec <= 0) return "N/D";

            return String.format(
                    Locale.US, "%.2f",
                    total * 8.0 / sec / 1e6);

        } catch (Exception e) {
            return "N/D";
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private String measureUpload() {
        HttpURLConnection c = null;

        try {
            byte[] data = new byte[500000];
            new java.security.SecureRandom().nextBytes(data);

            c = (HttpURLConnection) new URL(
                    "https://speed.cloudflare.com/__up"
            ).openConnection();

            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(10000);
            c.setReadTimeout(20000);
            c.setFixedLengthStreamingMode(data.length);

            c.setRequestProperty(
                    "Content-Type", "application/octet-stream");

            long t = System.nanoTime();

            OutputStream out = c.getOutputStream();
            out.write(data);
            out.flush();
            out.close();

            int code = c.getResponseCode();

            if (code < 200 || code >= 400) return "N/D";

            java.io.InputStream in = c.getInputStream();
            byte[] b = new byte[512];

            while (in.read(b) != -1) { }
            in.close();

            double sec = (System.nanoTime() - t) / 1e9;

            if (sec <= 0) return "N/D";

            return String.format(
                    Locale.US, "%.2f",
                    data.length * 8.0 / sec / 1e6);

        } catch (Exception e) {
            return "N/D";
        } finally {
            if (c != null) c.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refresh);
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refresh);
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        handler.removeCallbacks(refresh);
        super.onDestroy();
    }
}