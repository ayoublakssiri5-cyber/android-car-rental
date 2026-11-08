package com.example.locationvoitures.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.locationvoitures.R;
import com.example.locationvoitures.data.DatabaseHelper;
import com.example.locationvoitures.model.Payment;

import java.util.ArrayList;

public class PaymentListActivity extends Activity {
    private DatabaseHelper databaseHelper;
    private ArrayList<Payment> payments;
    private ArrayAdapter<Payment> adapter;
    private boolean canDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        databaseHelper = new DatabaseHelper(this);
        String role = getIntent().getStringExtra("role");
        canDelete = "Administrateur".equals(role);
        TextView textTitle = findViewById(R.id.textTitle);
        TextView chipAll = findViewById(R.id.chipAll);
        TextView chipDisponibles = findViewById(R.id.chipDisponibles);
        TextView chipRecents = findViewById(R.id.chipRecents);
        Button buttonAdd = findViewById(R.id.buttonAdd);
        ListView listItems = findViewById(R.id.listItems);

        textTitle.setText("Paiements");
        buttonAdd.setText("Enregistrer un paiement");
        chipDisponibles.setVisibility(View.GONE);

        payments = new ArrayList<>();
        adapter = new PaymentAdapter();
        listItems.setAdapter(adapter);

        buttonAdd.setOnClickListener(view ->
                startActivity(new Intent(this, PaymentFormActivity.class)));
        chipAll.setOnClickListener(view -> {
            setActiveChip(chipAll, chipRecents);
            loadPayments();
        });
        chipRecents.setOnClickListener(view -> {
            setActiveChip(chipRecents, chipAll);
            loadRecentPayments();
        });
        listItems.setOnItemLongClickListener((parent, view, position, id) -> {
            if (!canDelete) {
                Toast.makeText(this, "Suppression reservee a l'administrateur", Toast.LENGTH_SHORT).show();
                return true;
            }
            confirmDelete(payments.get(position));
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPayments();
    }

    private void loadPayments() {
        payments.clear();
        payments.addAll(databaseHelper.getPayments());
        adapter.notifyDataSetChanged();
    }

    private void loadRecentPayments() {
        payments.clear();
        payments.addAll(databaseHelper.getPayments());
        payments.sort((first, second) -> Integer.compare(second.getId(), first.getId()));
        adapter.notifyDataSetChanged();
    }

    private void setActiveChip(TextView active, TextView inactive) {
        active.setBackgroundResource(R.drawable.bg_chip_active);
        active.setTextColor(getColor(android.R.color.white));
        inactive.setBackgroundResource(R.drawable.bg_chip_inactive);
        inactive.setTextColor(getColor(R.color.textSecondary));
    }

    private void confirmDelete(Payment payment) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer")
                .setMessage("Supprimer ce paiement ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    databaseHelper.deletePayment(payment.getId());
                    Toast.makeText(this, "Paiement supprime", Toast.LENGTH_SHORT).show();
                    loadPayments();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private class PaymentAdapter extends ArrayAdapter<Payment> {
        PaymentAdapter() {
            super(PaymentListActivity.this, R.layout.item_list, R.id.textItem, payments);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(PaymentListActivity.this).inflate(R.layout.item_list, parent, false);
            }

            Payment payment = getItem(position);
            TextView textItem = view.findViewById(R.id.textItem);
            TextView buttonDeleteItem = view.findViewById(R.id.buttonDeleteItem);
            textItem.setText(payment == null ? "" : payment.toString());

            if (canDelete && payment != null) {
                buttonDeleteItem.setVisibility(View.VISIBLE);
                buttonDeleteItem.setOnClickListener(deleteView -> confirmDelete(payment));
            } else {
                buttonDeleteItem.setVisibility(View.GONE);
                buttonDeleteItem.setOnClickListener(null);
            }
            return view;
        }
    }
}
