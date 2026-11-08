package com.example.locationvoitures.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.locationvoitures.R;
import com.example.locationvoitures.data.DatabaseHelper;

public class DashboardActivity extends Activity {
    private TextView textStats;
    private DatabaseHelper databaseHelper;
    private String role;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        databaseHelper = new DatabaseHelper(this);
        username = getIntent().getStringExtra("username");
        role = getIntent().getStringExtra("role");
        if (role == null) {
            role = "Client";
        }
        TextView textWelcome = findViewById(R.id.textWelcome);
        textStats = findViewById(R.id.textStats);
        if (username != null && !username.isEmpty()) {
            textWelcome.setText("Tableau de bord - " + username + " (" + role + ")");
        }

        Button buttonCars = findViewById(R.id.buttonCars);
        Button buttonClients = findViewById(R.id.buttonClients);
        Button buttonReservations = findViewById(R.id.buttonReservations);
        Button buttonPayments = findViewById(R.id.buttonPayments);
        Button buttonPendingRequests = findViewById(R.id.buttonPendingRequests);
        Button buttonLogout = findViewById(R.id.buttonLogout);
        TextView navHome = findViewById(R.id.navHome);
        TextView navFleet = findViewById(R.id.navFleet);
        TextView navClients = findViewById(R.id.navClients);
        TextView navMore = findViewById(R.id.navMore);

        if ("Client".equals(role)) {
            buttonClients.setText("MON PROFIL");
            buttonPayments.setVisibility(View.GONE);
            navClients.setText("PROFIL");
            navMore.setVisibility(View.GONE);
            ((View) findViewById(R.id.textClientCount).getParent().getParent()).setVisibility(View.GONE);
            ((View) findViewById(R.id.textRevenue).getParent().getParent()).setVisibility(View.GONE);
            buttonReservations.setText("MES RESERVATIONS");
        }

        buttonCars.setOnClickListener(view -> openWithRole(CarListActivity.class));
        buttonClients.setOnClickListener(view -> {
            if ("Client".equals(role)) {
                openMyProfile();
            } else {
                openWithRole(ClientListActivity.class);
            }
        });
        buttonReservations.setOnClickListener(view -> openWithRole(ReservationListActivity.class));
        buttonPendingRequests.setOnClickListener(view -> openPendingReservations());
        buttonPayments.setOnClickListener(view -> openWithRole(PaymentListActivity.class));
        navHome.setOnClickListener(view -> recreate());
        navFleet.setOnClickListener(view -> openWithRole(CarListActivity.class));
        navClients.setOnClickListener(view -> {
            if ("Client".equals(role)) {
                openMyProfile();
            } else {
                openWithRole(ClientListActivity.class);
            }
        });
        navMore.setOnClickListener(view -> openWithRole(PaymentListActivity.class));
        buttonLogout.setOnClickListener(view -> {
            SharedPreferences preferences = getSharedPreferences("driveflow_session", MODE_PRIVATE);
            preferences.edit().clear().apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void openWithRole(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.putExtra("role", role);
        intent.putExtra("username", username);
        startActivity(intent);
    }

    private void openPendingReservations() {
        Intent intent = new Intent(this, ReservationListActivity.class);
        intent.putExtra("role", role);
        intent.putExtra("username", username);
        intent.putExtra("filter_status", "En attente");
        startActivity(intent);
    }

    private void openMyProfile() {
        Intent intent = new Intent(this, ClientFormActivity.class);
        intent.putExtra("client_id", databaseHelper.getOrCreateClientForUser(username).getId());
        intent.putExtra("role", role);
        intent.putExtra("username", username);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ("Client".equals(role)) {
            int clientId = databaseHelper.getOrCreateClientForUser(username).getId();
            int myReservations = databaseHelper.getReservationsForClient(clientId).size();
            int availableCars = databaseHelper.countAvailableCars();
            textStats.setText("Voitures disponibles: " + availableCars
                    + "\nMes reservations: " + myReservations);
            ((TextView) findViewById(R.id.textCarCount)).setText(String.valueOf(availableCars));
            ((TextView) findViewById(R.id.textResaCount)).setText(String.valueOf(myReservations));
        } else {
            int pending = databaseHelper.countReservationsByStatus("En attente");
            textStats.setText("Voitures: " + databaseHelper.countRows("voiture")
                    + "\nClients: " + databaseHelper.countRows("client")
                    + "\nReservations: " + databaseHelper.countRows("reservation")
                    + "\nDemandes en attente: " + pending
                    + "\nPaiements: " + databaseHelper.countRows("paiement"));
            ((TextView) findViewById(R.id.textCarCount)).setText(String.valueOf(databaseHelper.countRows("voiture")));
            ((TextView) findViewById(R.id.textClientCount)).setText(String.valueOf(databaseHelper.countRows("client")));
            ((TextView) findViewById(R.id.textResaCount)).setText(String.valueOf(databaseHelper.countRows("reservation")));
            ((TextView) findViewById(R.id.textRevenue)).setText(databaseHelper.countRows("paiement") + " paiement(s)");
            Button buttonPendingRequests = findViewById(R.id.buttonPendingRequests);
            buttonPendingRequests.setVisibility(View.VISIBLE);
            buttonPendingRequests.setEnabled(true);
            buttonPendingRequests.setText("Demandes en attente (" + pending + ")");
        }
    }
}
