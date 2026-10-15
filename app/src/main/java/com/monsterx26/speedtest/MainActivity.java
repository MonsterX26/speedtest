
package com.monsterx26.speedtest;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView statusText;

    private static final String ACTION_START =
            "com.monsterx26.speedtest.START";
    private static final String ACTION_STOP =
            "com.monsterx26.speedtest.STOP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(24, 24, 24, 24);
        layout.setBackgroundColor(Color.rgb(18, 24, 35));

        TextView title = new TextView(this);
        title.setText("Speed Lite+");
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 24);
        layout.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        statusText = new TextView(this);
        statusText.setText("Moniteur de vitesse Internet");
        statusText.setTextSize(17);
        statusText.setTextColor(Color.LTGRAY);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 30);
        layout.addView(statusText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button startButton = new Button(this);
        startButton.setText("ACTIVER LA VITESSE");
        layout.addView(startButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button stopButton = new Button(this);
        stopButton.setText("ARRÊTER LE MONITEUR");
        LinearLayout.LayoutParams stopParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        stopParams.topMargin = 12;
        layout.addView(stopButton, stopParams);

        TextView info = new TextView(this);
        info.setText(
                "Le moniteur mesure le trafic réseau de l'appareil.\n\n" +
                "Après activation, vérifie la notification persistante " +
                "et l'affichage dans la barre d'état."
        );
        info.setTextSize(14);
        info.setTextColor(Color.LTGRAY);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 30, 0, 0);
        layout.addView(info, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(layout);

        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this, SpeedService.class);
            intent.setAction(ACTION_START);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            statusText.setText("Moniteur activé");
        });

        stopButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this, SpeedService.class);
            intent.setAction(ACTION_STOP);
            startService(intent);

            statusText.setText("Moniteur arrêté");
        });
    }
}
