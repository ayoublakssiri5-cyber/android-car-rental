package com.example.locationvoitures.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.example.locationvoitures.R;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences preferences = getSharedPreferences("driveflow_session", MODE_PRIVATE);
            String username = preferences.getString("username", null);
            String role = preferences.getString("role", null);
            Intent intent;
            if (username != null && role != null) {
                intent = new Intent(this, DashboardActivity.class);
                intent.putExtra("username", username);
                intent.putExtra("role", role);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, 900);
    }
}
