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
import com.example.locationvoitures.model.Car;
import com.example.locationvoitures.model.Reservation;

import java.util.ArrayList;

public class ReservationListActivity extends Activity {
    private DatabaseHelper databaseHelper;
    private ArrayList<Reservation> reservations;
    private ArrayAdapter<Reservation> adapter;
    private boolean isClientRole;
    private boolean canDelete;
    private String role;
    private String username;
    private String activeStatusFilter = "all";
    private TextView textSubtitle;
    private TextView chipAll;
    private TextView chipPending;
    private TextView chipRecents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        databaseHelper = new DatabaseHelper(this);
        role = getIntent().getStringExtra("role");
        username = getIntent().getStringExtra("username");
        isClientRole = "Client".equals(role);
        canDelete = "Administrateur".equals(role);
        TextView textTitle = findViewById(R.id.textTitle);
        textSubtitle = findViewById(R.id.textSubtitle);
        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipDisponibles);
        chipRecents = findViewById(R.id.chipRecents);
        Button buttonAdd = findViewById(R.id.buttonAdd);
        ListView listItems = findViewById(R.id.listItems);

        textTitle.setText(isClientRole ? "Mes reservations" : "Reservations");
        buttonAdd.setText(isClientRole ? "Nouvelle demande" : "Nouvelle reservation");
        chipPending.setText("En attente");
        activeStatusFilter = getIntent().getStringExtra("filter_status") == null
                ? "all"
                : getIntent().getStringExtra("filter_status");
        if ("En attente".equals(activeStatusFilter)) {
            setActiveChip(chipPending, chipAll, chipRecents);
        }

        reservations = new ArrayList<>();
        adapter = new ReservationAdapter();
        listItems.setAdapter(adapter);

        buttonAdd.setOnClickListener(view ->
                openReservationForm(-1));
        chipAll.setOnClickListener(view -> {
            activeStatusFilter = "all";
            setActiveChip(chipAll, chipPending, chipRecents);
            loadReservations();
        });
        chipPending.setOnClickListener(view -> {
            activeStatusFilter = "En attente";
            setActiveChip(chipPending, chipAll, chipRecents);
            loadReservations();
        });
        chipRecents.setOnClickListener(view -> {
            activeStatusFilter = "recent";
            setActiveChip(chipRecents, chipAll, chipPending);
            loadRecentReservations();
        });
        listItems.setOnItemClickListener((parent, view, position, id) -> {
            if (isClientRole) {
                showClientReservationDetails(reservations.get(position));
                return;
            }
            openReservationForm(reservations.get(position).getId());
        });
        listItems.setOnItemLongClickListener((parent, view, position, id) -> {
            if (!canDelete) {
                if (!isClientRole) {
                    Toast.makeText(this, "Suppression reservee a l'administrateur", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            confirmDelete(reservations.get(position));
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReservations();
    }

    private void loadReservations() {
        reservations.clear();
        if (isClientRole) {
            for (Reservation reservation : databaseHelper.getReservationsForClient(
                    databaseHelper.getOrCreateClientForUser(username).getId())) {
                if (shouldShowReservation(reservation)) {
                    reservations.add(reservation);
                }
            }
        } else {
            for (Reservation reservation : databaseHelper.getReservations()) {
                if (shouldShowReservation(reservation)) {
                    reservations.add(reservation);
                }
            }
        }
        updateSubtitle();
        adapter.notifyDataSetChanged();
    }

    private void loadRecentReservations() {
        reservations.clear();
        if (isClientRole) {
            reservations.addAll(databaseHelper.getReservationsForClient(
                    databaseHelper.getOrCreateClientForUser(username).getId()));
        } else {
            reservations.addAll(databaseHelper.getReservations());
        }
        if ("En attente".equals(activeStatusFilter)) {
            reservations.removeIf(reservation -> !"En attente".equals(reservation.getStatut()));
        }
        updateSubtitle();
        reservations.sort((first, second) -> Integer.compare(second.getId(), first.getId()));
        adapter.notifyDataSetChanged();
    }

    private boolean shouldShowReservation(Reservation reservation) {
        return "all".equals(activeStatusFilter)
                || "recent".equals(activeStatusFilter)
                || activeStatusFilter.equals(reservation.getStatut());
    }

    private void updateSubtitle() {
        int pending = databaseHelper.countReservationsByStatus("En attente");
        textSubtitle.setText(reservations.size() + " affichee(s) - " + pending + " en attente");
    }

    private void openReservationForm(int reservationId) {
        Intent intent = new Intent(this, ReservationFormActivity.class);
        intent.putExtra("role", role);
        intent.putExtra("username", username);
        if (reservationId != -1) {
            intent.putExtra("reservation_id", reservationId);
        }
        startActivity(intent);
    }

    private void setActiveChip(TextView active, TextView firstInactive, TextView secondInactive) {
        active.setBackgroundResource(R.drawable.bg_chip_active);
        active.setTextColor(getColor(android.R.color.white));
        firstInactive.setBackgroundResource(R.drawable.bg_chip_inactive);
        firstInactive.setTextColor(getColor(R.color.textSecondary));
        secondInactive.setBackgroundResource(R.drawable.bg_chip_inactive);
        secondInactive.setTextColor(getColor(R.color.textSecondary));
    }

    private void confirmDelete(Reservation reservation) {
        if (databaseHelper.reservationHasPayments(reservation.getId())) {
            Toast.makeText(this, "Impossible: cette reservation a des paiements. Supprimez d'abord les paiements.", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Supprimer")
                .setMessage("Supprimer cette reservation ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    Car car = databaseHelper.getCarById(reservation.getVoitureId());
                    databaseHelper.deleteReservation(reservation.getId());
                    if (car != null) {
                        NotificationUtils.notifyCarAvailability(this, car.getMarque() + " " + car.getModele(), true);
                    }
                    Toast.makeText(this, "Reservation supprimee", Toast.LENGTH_SHORT).show();
                    loadReservations();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private void confirmReservation(Reservation reservation) {
        Car car = databaseHelper.getCarById(reservation.getVoitureId());
        if (car == null) {
            Toast.makeText(this, "Voiture introuvable", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!car.isDisponible()) {
            Toast.makeText(this, "Cette voiture est marquee indisponible par l'agence", Toast.LENGTH_SHORT).show();
            return;
        }
        if (databaseHelper.hasConfirmedReservationOverlap(
                reservation.getVoitureId(),
                reservation.getDateDebut(),
                reservation.getDateFin(),
                reservation.getId())) {
            Toast.makeText(this, "Conflit: voiture deja reservee sur ces dates", Toast.LENGTH_LONG).show();
            return;
        }

        boolean success = databaseHelper.updateReservation(
                reservation.getId(),
                reservation.getClientId(),
                reservation.getVoitureId(),
                reservation.getDateDebut(),
                reservation.getDateFin(),
                reservation.getPrixTotal(),
                "Confirmee"
        );
        if (success) {
            NotificationUtils.notifyCarAvailability(this, car.getMarque() + " " + car.getModele(), false);
            Toast.makeText(this, "Reservation confirmee", Toast.LENGTH_SHORT).show();
            loadReservations();
        } else {
            Toast.makeText(this, "Erreur de confirmation", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelAgencyReservation(Reservation reservation) {
        Car car = databaseHelper.getCarById(reservation.getVoitureId());
        boolean success = databaseHelper.updateReservation(
                reservation.getId(),
                reservation.getClientId(),
                reservation.getVoitureId(),
                reservation.getDateDebut(),
                reservation.getDateFin(),
                reservation.getPrixTotal(),
                "Annulee"
        );
        if (success) {
            if (car != null) {
                NotificationUtils.notifyCarAvailability(this, car.getMarque() + " " + car.getModele(), true);
            }
            Toast.makeText(this, "Reservation annulee", Toast.LENGTH_SHORT).show();
            loadReservations();
        } else {
            Toast.makeText(this, "Erreur d'annulation", Toast.LENGTH_SHORT).show();
        }
    }

    private void showClientReservationDetails(Reservation reservation) {
        String message = reservation.toString()
                + "\n\nVotre demande est traitee par l'agence.";

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Detail reservation")
                .setMessage(message)
                .setNegativeButton("Fermer", null);

        if ("En attente".equals(reservation.getStatut())) {
            builder.setPositiveButton("Annuler la demande", (dialog, which) -> cancelClientReservation(reservation));
        }
        builder.show();
    }

    private void cancelClientReservation(Reservation reservation) {
        boolean success = databaseHelper.updateReservation(
                reservation.getId(),
                reservation.getClientId(),
                reservation.getVoitureId(),
                reservation.getDateDebut(),
                reservation.getDateFin(),
                reservation.getPrixTotal(),
                "Annulee"
        );
        if (success) {
            Toast.makeText(this, "Demande annulee", Toast.LENGTH_SHORT).show();
            loadReservations();
        } else {
            Toast.makeText(this, "Erreur d'annulation", Toast.LENGTH_SHORT).show();
        }
    }

    private class ReservationAdapter extends ArrayAdapter<Reservation> {
        ReservationAdapter() {
            super(ReservationListActivity.this, R.layout.item_list, R.id.textItem, reservations);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(ReservationListActivity.this).inflate(R.layout.item_list, parent, false);
            }

            Reservation reservation = getItem(position);
            TextView textItem = view.findViewById(R.id.textItem);
            TextView buttonConfirmItem = view.findViewById(R.id.buttonConfirmItem);
            TextView buttonCancelItem = view.findViewById(R.id.buttonCancelItem);
            TextView buttonDeleteItem = view.findViewById(R.id.buttonDeleteItem);
            textItem.setText(reservation == null ? "" : reservation.toString());

            if (!isClientRole && reservation != null && "En attente".equals(reservation.getStatut())) {
                buttonConfirmItem.setVisibility(View.VISIBLE);
                buttonCancelItem.setVisibility(View.VISIBLE);
                buttonConfirmItem.setOnClickListener(confirmView -> confirmReservation(reservation));
                buttonCancelItem.setOnClickListener(cancelView -> cancelAgencyReservation(reservation));
            } else {
                buttonConfirmItem.setVisibility(View.GONE);
                buttonCancelItem.setVisibility(View.GONE);
                buttonConfirmItem.setOnClickListener(null);
                buttonCancelItem.setOnClickListener(null);
            }

            if (canDelete && reservation != null) {
                buttonDeleteItem.setVisibility(View.VISIBLE);
                buttonDeleteItem.setOnClickListener(deleteView -> confirmDelete(reservation));
            } else {
                buttonDeleteItem.setVisibility(View.GONE);
                buttonDeleteItem.setOnClickListener(null);
            }
            return view;
        }
    }
}
