package com.example.locationvoitures.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.locationvoitures.R;
import com.example.locationvoitures.data.DatabaseHelper;

public class RegisterActivity extends Activity {
    private EditText editUsername;
    private EditText editPassword;
    private Spinner spinnerRole;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        databaseHelper = new DatabaseHelper(this);
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        Button buttonRegister = findViewById(R.id.buttonRegister);

        String[] roles = {"Client"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roles
        );
        spinnerRole.setAdapter(adapter);
        spinnerRole.setEnabled(false);

        buttonRegister.setOnClickListener(view -> register());
    }

    private void register() {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString();
        String role = "Client";

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 4) {
            Toast.makeText(this, "Mot de passe trop court", Toast.LENGTH_SHORT).show();
            return;
        }

        if (databaseHelper.registerUser(username, password, role)) {
            Toast.makeText(this, "Compte cree", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Nom utilisateur deja utilise", Toast.LENGTH_SHORT).show();
        }
    }
}
