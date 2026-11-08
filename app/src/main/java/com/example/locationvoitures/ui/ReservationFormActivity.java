package com.example.locationvoitures.ui;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.locationvoitures.R;
import com.example.locationvoitures.data.DatabaseHelper;
import com.example.locationvoitures.data.RentalRules;
import com.example.locationvoitures.model.Car;
import com.example.locationvoitures.model.Client;
import com.example.locationvoitures.model.Reservation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReservationFormActivity extends Activity {
    private static final String RESERVATION_CHANNEL_ID = "driveflow_reservations";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 77;

    private Spinner spinnerClient;
    private Spinner spinnerCar;
    private Spinner spinnerStatut;
    private EditText editDateDebut;
    private EditText editDateFin;
    private DatabaseHelper databaseHelper;
    private ArrayList<Client> clients;
    private ArrayList<Car> cars;
    private int reservationId = -1;
    private int preselectedCarId = -1;
    private Button buttonCancelReservation;
    private Reservation currentReservation;
    private boolean isClientRole;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_form);

        databaseHelper = new DatabaseHelper(this);
        reservationId = getIntent().getIntExtra("reservation_id", -1);
        preselectedCarId = getIntent().getIntExtra("car_id", -1);
        String role = getIntent().getStringExtra("role");
        username = getIntent().getStringExtra("username");
        isClientRole = "Client".equals(role);

        TextView textTitle = findViewById(R.id.textTitle);
        spinnerClient = findViewById(R.id.spinnerClient);
        spinnerCar = findViewById(R.id.spinnerCar);
        spinnerStatut = findViewById(R.id.spinnerStatut);
        editDateDebut = findViewById(R.id.editDateDebut);
        editDateFin = findViewById(R.id.editDateFin);
        Button buttonSave = findViewById(R.id.buttonSave);
        buttonCancelReservation = findViewById(R.id.buttonCancelReservation);

        createNotificationChannel();
        requestNotificationPermissionIfNeeded();
        textTitle.setText(isClientRole
                ? "Demande de reservation"
                : (reservationId == -1 ? "Nouvelle reservation" : "Modifier reservation"));
        loadSpinners();
        if (reservationId != -1) {
            loadReservation();
        } else if (preselectedCarId != -1) {
            selectCar(preselectedCarId);
        }

        editDateDebut.setOnClickListener(view -> showDatePicker(editDateDebut));
        editDateFin.setOnClickListener(view -> showDatePicker(editDateFin));
        buttonSave.setOnClickListener(view -> saveReservation());
        buttonCancelReservation.setOnClickListener(view -> cancelReservation());
    }

    private void loadSpinners() {
        if (isClientRole) {
            clients = new ArrayList<>();
            clients.add(databaseHelper.getOrCreateClientForUser(username));
            cars = new ArrayList<>();
            for (Car car : databaseHelper.getCars()) {
                if (car.isDisponible()) {
                    cars.add(car);
                }
            }
        } else {
            clients = databaseHelper.getClients();
            cars = databaseHelper.getCars();
        }

        spinnerClient.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, clients));
        spinnerCar.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cars));
        spinnerStatut.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                isClientRole
                        ? new String[]{"En attente"}
                        : new String[]{"En attente", "Confirmee", "Annulee", "Terminee"}));
        if (isClientRole) {
            spinnerClient.setEnabled(false);
            spinnerStatut.setEnabled(false);
            if (preselectedCarId != -1) {
                spinnerCar.setEnabled(false);
            }
        }
    }

    private void loadReservation() {
        currentReservation = databaseHelper.getReservationById(reservationId);
        if (currentReservation == null) {
            Toast.makeText(this, "Reservation introuvable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editDateDebut.setText(currentReservation.getDateDebut());
        editDateFin.setText(currentReservation.getDateFin());
        selectClient(currentReservation.getClientId());
        selectCar(currentReservation.getVoitureId());
        selectStatus(currentReservation.getStatut());
        if (!"Annulee".equals(currentReservation.getStatut())) {
            buttonCancelReservation.setVisibility(View.VISIBLE);
        }
    }

    private void saveReservation() {
        if (clients.isEmpty() || cars.isEmpty()) {
            Toast.makeText(this, isClientRole
                    ? "Aucune voiture disponible pour le moment"
                    : "Ajoutez d'abord un client et une voiture", Toast.LENGTH_LONG).show();
            return;
        }

        Client client = (Client) spinnerClient.getSelectedItem();
        Car car = (Car) spinnerCar.getSelectedItem();
        String dateDebut = editDateDebut.getText().toString().trim();
        String dateFin = editDateFin.getText().toString().trim();
        String statut = isClientRole ? "En attente" : spinnerStatut.getSelectedItem().toString();
        long days = calculateDays(dateDebut, dateFin);

        if (days <= 0) {
            Toast.makeText(this, "Selectionnez une date de depart et une date d'arrivee valides", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isPastDate(dateDebut)) {
            Toast.makeText(this, "La date de depart ne peut pas etre dans le passe", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!car.isDisponible()) {
            Toast.makeText(this, "Cette voiture est marquee indisponible par l'agence", Toast.LENGTH_SHORT).show();
            return;
        }
        if (databaseHelper.hasConfirmedReservationOverlap(car.getId(), dateDebut, dateFin, reservationId)) {
            Toast.makeText(this, "Cette voiture est deja reservee sur ces dates", Toast.LENGTH_LONG).show();
            return;
        }

        double prixTotal = RentalRules.totalPrice(days, car.getPrixJour());
        boolean success;
        if (reservationId == -1) {
            success = databaseHelper.addReservation(client.getId(), car.getId(), dateDebut, dateFin, prixTotal, statut) != -1;
        } else {
            success = databaseHelper.updateReservation(reservationId, client.getId(), car.getId(), dateDebut, dateFin, prixTotal, statut);
        }

        if (success) {
            Toast.makeText(this,
                    isClientRole
                            ? "Demande envoyee a l'administrateur"
                            : "Reservation enregistree: " + prixTotal + " DH",
                    Toast.LENGTH_LONG).show();
            showReservationNotification(client, car, prixTotal, reservationId == -1, statut);
            if ("Confirmee".equals(statut)) {
                NotificationUtils.notifyCarAvailability(this, car.getMarque() + " " + car.getModele(), false);
            } else if ("Annulee".equals(statut) || "Terminee".equals(statut)) {
                NotificationUtils.notifyCarAvailability(this, car.getMarque() + " " + car.getModele(), true);
            }
            finish();
        } else {
            Toast.makeText(this, "Erreur d'enregistrement", Toast.LENGTH_SHORT).show();
        }
    }

    private long calculateDays(String start, String end) {
        return RentalRules.daysBetween(start, end);
    }

    private boolean isPastDate(String value) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            format.setLenient(false);
            Date selected = format.parse(value);
            Date today = format.parse(format.format(new Date()));
            return selected != null && today != null && selected.before(today);
        } catch (ParseException exception) {
            return true;
        }
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        String currentValue = target.getText().toString().trim();
        if (!currentValue.isEmpty()) {
            try {
                Date currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(currentValue);
                if (currentDate != null) {
                    calendar.setTime(currentDate);
                }
            } catch (ParseException ignored) {
                // Keep today's date if the stored text is not parseable.
            }
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    target.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void cancelReservation() {
        if (currentReservation == null) {
            return;
        }

        boolean success = databaseHelper.updateReservation(
                currentReservation.getId(),
                currentReservation.getClientId(),
                currentReservation.getVoitureId(),
                currentReservation.getDateDebut(),
                currentReservation.getDateFin(),
                currentReservation.getPrixTotal(),
                "Annulee"
        );
        if (success) {
            Toast.makeText(this, "Reservation annulee", Toast.LENGTH_SHORT).show();
            Car car = databaseHelper.getCarById(currentReservation.getVoitureId());
            if (car != null) {
                NotificationUtils.notifyCarAvailability(this, car.getMarque() + " " + car.getModele(), true);
            }
            finish();
        } else {
            Toast.makeText(this, "Erreur d'annulation", Toast.LENGTH_SHORT).show();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                RESERVATION_CHANNEL_ID,
                "Reservations DriveFlow",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Notifications de creation et modification des reservations");

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void showReservationNotification(Client client, Car car, double prixTotal, boolean isNewReservation, String statut) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(this, ReservationListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "En attente".equals(statut)
                ? "Demande client en attente"
                : (isNewReservation ? "Nouvelle reservation" : "Reservation mise a jour");
        String message = client.toString() + " - " + car.getMarque() + " " + car.getModele()
                + " (" + String.format(Locale.US, "%.0f DH", prixTotal) + ") - " + statut;

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, RESERVATION_CHANNEL_ID)
                : new Notification.Builder(this);

        Notification notification = builder
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), notification);
        }
    }

    private void selectClient(int clientId) {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).getId() == clientId) {
                spinnerClient.setSelection(i);
                return;
            }
        }
    }

    private void selectCar(int carId) {
        for (int i = 0; i < cars.size(); i++) {
            if (cars.get(i).getId() == carId) {
                spinnerCar.setSelection(i);
                return;
            }
        }
    }

    private void selectStatus(String status) {
        ArrayAdapter adapter = (ArrayAdapter) spinnerStatut.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (status.equals(adapter.getItem(i))) {
                spinnerStatut.setSelection(i);
                return;
            }
        }
    }
}
