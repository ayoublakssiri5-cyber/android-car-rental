package com.example.locationvoitures.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.locationvoitures.R;
import com.example.locationvoitures.data.DatabaseHelper;
import com.example.locationvoitures.model.Client;

public class ClientFormActivity extends Activity {
    private EditText editNom;
    private EditText editPrenom;
    private EditText editEmail;
    private EditText editTelephone;
    private DatabaseHelper databaseHelper;
    private int clientId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_form);

        databaseHelper = new DatabaseHelper(this);
        clientId = getIntent().getIntExtra("client_id", -1);

        TextView textTitle = findViewById(R.id.textTitle);
        editNom = findViewById(R.id.editNom);
        editPrenom = findViewById(R.id.editPrenom);
        editEmail = findViewById(R.id.editEmail);
        editTelephone = findViewById(R.id.editTelephone);
        Button buttonSave = findViewById(R.id.buttonSave);

        textTitle.setText(clientId == -1 ? "Ajouter un client" : "Modifier un client");
        if (clientId != -1) {
            loadClient();
        }

        buttonSave.setOnClickListener(view -> saveClient());
    }

    private void loadClient() {
        Client client = databaseHelper.getClientById(clientId);
        if (client == null) {
            Toast.makeText(this, "Client introuvable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editNom.setText(client.getNom());
        editPrenom.setText(client.getPrenom());
        editEmail.setText(client.getEmail());
        editTelephone.setText(client.getTelephone());
    }

    private void saveClient() {
        String nom = editNom.getText().toString().trim();
        String prenom = editPrenom.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String telephone = editTelephone.getText().toString().trim();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || telephone.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email invalide", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!telephone.matches("[+0-9 ]{8,16}")) {
            Toast.makeText(this, "Telephone invalide", Toast.LENGTH_SHORT).show();
            return;
        }
        if (databaseHelper.isClientEmailUsed(email, clientId)) {
            Toast.makeText(this, "Email deja utilise par un autre client", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success;
        if (clientId == -1) {
            success = databaseHelper.addClient(nom, prenom, email, telephone) != -1;
        } else {
            success = databaseHelper.updateClient(clientId, nom, prenom, email, telephone);
        }

        if (success) {
            Toast.makeText(this, "Client enregistre", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Erreur d'enregistrement", Toast.LENGTH_SHORT).show();
        }
    }
}
