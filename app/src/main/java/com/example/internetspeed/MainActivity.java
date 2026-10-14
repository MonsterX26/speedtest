package com.example.internetspeed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

public class MainActivity extends Activity {
    private TextView speed, status;
    private final android.os.Handler handler = new android.os.Handler();
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            long rx = android.net.TrafficStats.getTotalRxBytes();
            long tx = android.net.TrafficStats.getTotalTxBytes();
            // Le service publie la vitesse dans les préférences partagées.
            android.content.SharedPreferences p = getSharedPreferences("speed", MODE_PRIVATE);
            speed.setText(p.getString("value", "Mesure en cours…"));
            status.setText(p.getBoolean("running", false) ? "Surveillance active" : "Appuie sur Démarrer");
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(12, 20, 38));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(28, 30, 28, 30);
        root.setBackgroundColor(Color.rgb(12, 20, 38));

        TextView title = new TextView(this);
        title.setText("VITESSE INTERNET");
        title.setTextColor(Color.WHITE); title.setTextSize(23);
        title.setGravity(Gravity.CENTER);
        speed = new TextView(this);
        speed.setText("0 Ko/s"); speed.setTextColor(Color.rgb(72, 220, 170));
        speed.setTextSize(42); speed.setGravity(Gravity.CENTER);
        status = new TextView(this);
        status.setTextColor(Color.LTGRAY); status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        TextView note = new TextView(this);
        note.setText("Débit total reçu + envoyé\nLa vitesse varie selon l’activité du téléphone.");
        note.setTextColor(Color.LTGRAY); note.setTextSize(14); note.setGravity(Gravity.CENTER);
        Button start = new Button(this); start.setText("DÉMARRER");
        Button stop = new Button(this); stop.setText("ARRÊTER");
        start.setOnClickListener(v -> {
            Intent i = new Intent(this, SpeedService.class);
            i.setAction("START");
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        });
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, SpeedService.class));
            getSharedPreferences("speed", MODE_PRIVATE).edit().putBoolean("running", false).apply();
            status.setText("Surveillance arrêtée");
        });
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));
        addSpace(root, 28);
        root.addView(speed, new LinearLayout.LayoutParams(-1, -2));
        addSpace(root, 12);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));
        addSpace(root, 24);
        root.addView(start, new LinearLayout.LayoutParams(-1, -2));
        root.addView(stop, new LinearLayout.LayoutParams(-1, -2));
        addSpace(root, 18);
        root.addView(note, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }
    private void addSpace(LinearLayout root, int dp) {
        TextView space = new TextView(this);
        space.setHeight((int)(dp * getResources().getDisplayMetrics().density));
        root.addView(space);
    }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(refresh); }
}
