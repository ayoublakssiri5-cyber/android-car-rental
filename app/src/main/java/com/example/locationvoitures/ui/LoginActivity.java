package com.example.locationvoitures.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.locationvoitures.R;
import com.example.locationvoitures.data.DatabaseHelper;

public class LoginActivity extends Activity {
    private EditText editUsername;
    private EditText editPassword;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        databaseHelper = new DatabaseHelper(this);
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonGoRegister = findViewById(R.id.buttonGoRegister);

        buttonLogin.setOnClickListener(view -> login());
        buttonGoRegister.setOnClickListener(view ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void login() {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        if (databaseHelper.isValidLogin(username, password)) {
            String role = databaseHelper.getUserRole(username);
            SharedPreferences preferences = getSharedPreferences("driveflow_session", MODE_PRIVATE);
            preferences.edit()
                    .putString("username", username)
                    .putString("role", role)
                    .apply();

            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("username", username);
            intent.putExtra("role", role);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Identifiants incorrects", Toast.LENGTH_SHORT).show();
        }
    }
}
