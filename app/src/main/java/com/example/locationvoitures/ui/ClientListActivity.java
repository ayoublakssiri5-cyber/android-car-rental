package com.example.locationvoitures.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.locationvoitures.R;
import com.example.locationvoitures.data.DatabaseHelper;
import com.example.locationvoitures.model.Client;

import java.util.ArrayList;

public class ClientListActivity extends Activity {
    private DatabaseHelper databaseHelper;
    private ArrayList<Client> clients;
    private ArrayAdapter<Client> adapter;
    private boolean canManage;
    private boolean canDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        databaseHelper = new DatabaseHelper(this);
        String role = getIntent().getStringExtra("role");
        canManage = !"Client".equals(role);
        canDelete = "Administrateur".equals(role);
        TextView textTitle = findViewById(R.id.textTitle);
        TextView chipAll = findViewById(R.id.chipAll);
        TextView chipDisponibles = findViewById(R.id.chipDisponibles);
        TextView chipRecents = findViewById(R.id.chipRecents);
        Button buttonAdd = findViewById(R.id.buttonAdd);
        ListView listItems = findViewById(R.id.listItems);

        textTitle.setText("Clients");
        buttonAdd.setText("Ajouter un client");
        chipDisponibles.setVisibility(View.GONE);
        if (!canManage) {
            buttonAdd.setVisibility(View.GONE);
        }

        clients = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, R.layout.item_list, R.id.textItem, clients);
        listItems.setAdapter(adapter);

        buttonAdd.setOnClickListener(view ->
                startActivity(new Intent(this, ClientFormActivity.class)));
        chipAll.setOnClickListener(view -> {
            setActiveChip(chipAll, chipRecents);
            loadClients();
        });
        chipRecents.setOnClickListener(view -> {
            setActiveChip(chipRecents, chipAll);
            loadRecentClients();
        });
        listItems.setOnItemClickListener((parent, view, position, id) -> {
            if (!canManage) {
                return;
            }
            Intent intent = new Intent(this, ClientFormActivity.class);
            intent.putExtra("client_id", clients.get(position).getId());
            startActivity(intent);
        });
        listItems.setOnItemLongClickListener((parent, view, position, id) -> {
            if (!canDelete) {
                if (canManage) {
                    Toast.makeText(this, "Suppression reservee a l'administrateur", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            confirmDelete(clients.get(position));
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClients();
    }

    private void loadClients() {
        clients.clear();
        clients.addAll(databaseHelper.getClients());
        adapter.notifyDataSetChanged();
    }

    private void loadRecentClients() {
        clients.clear();
        clients.addAll(databaseHelper.getClients());
        clients.sort((first, second) -> Integer.compare(second.getId(), first.getId()));
        adapter.notifyDataSetChanged();
    }

    private void setActiveChip(TextView active, TextView inactive) {
        active.setBackgroundResource(R.drawable.bg_chip_active);
        active.setTextColor(getColor(android.R.color.white));
        inactive.setBackgroundResource(R.drawable.bg_chip_inactive);
        inactive.setTextColor(getColor(R.color.textSecondary));
    }

    private void confirmDelete(Client client) {
        if (databaseHelper.clientHasReservations(client.getId())) {
            Toast.makeText(this, "Impossible: ce client a des reservations", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Supprimer")
                .setMessage("Supprimer " + client.getPrenom() + " " + client.getNom() + " ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    databaseHelper.deleteClient(client.getId());
                    Toast.makeText(this, "Client supprime", Toast.LENGTH_SHORT).show();
                    loadClients();
                })
                .setNegativeButton("Non", null)
                .show();
    }
}
