package com.example.locationvoitures.ui;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.locationvoitures.R;
import com.example.locationvoitures.data.DatabaseHelper;
import com.example.locationvoitures.model.Reservation;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PaymentFormActivity extends AppCompatActivity {
    private static final String PAYMENT_CHANNEL_ID = "driveflow_payments";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 78;
    private static final String STRIPE_BACKEND_URL = "http://10.0.2.2:4242/payment-sheet";

    private Spinner spinnerReservation;
    private EditText editMontant;
    private EditText editDatePaiement;
    private DatabaseHelper databaseHelper;
    private ArrayList<Reservation> reservations;
    private PaymentSheet paymentSheet;
    private PaymentInput pendingStripePayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_form);

        databaseHelper = new DatabaseHelper(this);
        spinnerReservation = findViewById(R.id.spinnerReservation);
        editMontant = findViewById(R.id.editMontant);
        editDatePaiement = findViewById(R.id.editDatePaiement);
        Button buttonSave = findViewById(R.id.buttonSave);
        Button buttonOnlinePay = findViewById(R.id.buttonOnlinePay);

        paymentSheet = new PaymentSheet.Builder(this::onPaymentSheetResult).build(this);
        createNotificationChannel();
        requestNotificationPermissionIfNeeded();
        reservations = databaseHelper.getConfirmedReservationsWithBalance();
        spinnerReservation.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, reservations));
        if (editDatePaiement.getText().toString().trim().isEmpty()) {
            editDatePaiement.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
        }
        buttonSave.setOnClickListener(view -> savePayment());
        buttonOnlinePay.setOnClickListener(view -> startStripePayment());
    }

    private void savePayment() {
        PaymentInput input = readPaymentInput();
        if (input == null) {
            return;
        }

        savePaymentRecord(input.reservation, input.montant, input.datePaiement, "Paiement enregistre");
    }

    private void startStripePayment() {
        PaymentInput input = readPaymentInput();
        if (input == null) {
            return;
        }
        pendingStripePayment = input;
        Toast.makeText(this, "Connexion a Stripe...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                JSONObject requestJson = new JSONObject();
                requestJson.put("amount", Math.round(input.montant * 100));
                requestJson.put("currency", "mad");
                requestJson.put("reservationId", input.reservation.getId());

                URL url = new URL(STRIPE_BACKEND_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setDoOutput(true);

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(requestJson.toString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        responseCode >= 200 && responseCode < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream(),
                        StandardCharsets.UTF_8
                ));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();

                if (responseCode < 200 || responseCode >= 300) {
                    throw new IllegalStateException(responseBuilder.toString());
                }

                JSONObject responseJson = new JSONObject(responseBuilder.toString());
                String publishableKey = responseJson.getString("publishableKey");
                String paymentIntentClientSecret = responseJson.getString("paymentIntent");

                runOnUiThread(() -> {
                    PaymentConfiguration.init(getApplicationContext(), publishableKey);
                    PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("DriveFlow")
                            .allowsDelayedPaymentMethods(true)
                            .build();
                    paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "Stripe indisponible: demarrez le backend ou verifiez les cles",
                        Toast.LENGTH_LONG
                ).show());
            }
        }).start();
    }

    private void onPaymentSheetResult(PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            if (pendingStripePayment != null) {
                savePaymentRecord(
                        pendingStripePayment.reservation,
                        pendingStripePayment.montant,
                        pendingStripePayment.datePaiement,
                        "Paiement Stripe valide"
                );
            }
        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(this, "Paiement annule", Toast.LENGTH_SHORT).show();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            Toast.makeText(this, "Paiement refuse par Stripe", Toast.LENGTH_LONG).show();
        }
    }

    private PaymentInput readPaymentInput() {
        if (reservations.isEmpty()) {
            Toast.makeText(this, "Aucune reservation confirmee avec reste a payer", Toast.LENGTH_LONG).show();
            return null;
        }

        String montantText = editMontant.getText().toString().trim();
        String datePaiement = editDatePaiement.getText().toString().trim();
        if (montantText.isEmpty() || !isValidDate(datePaiement)) {
            Toast.makeText(this, "Montant ou date invalide", Toast.LENGTH_SHORT).show();
            return null;
        }

        double montant;
        try {
            montant = Double.parseDouble(montantText);
        } catch (NumberFormatException exception) {
            Toast.makeText(this, "Montant invalide", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (montant <= 0) {
            Toast.makeText(this, "Montant invalide", Toast.LENGTH_SHORT).show();
            return null;
        }

        Reservation reservation = (Reservation) spinnerReservation.getSelectedItem();
        if (!"Confirmee".equals(reservation.getStatut())) {
            Toast.makeText(this, "Paiement possible seulement pour une reservation confirmee", Toast.LENGTH_SHORT).show();
            return null;
        }

        double remaining = databaseHelper.getRemainingForReservation(reservation);
        if (remaining <= 0) {
            Toast.makeText(this, "Cette reservation est deja payee", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (montant > remaining) {
            Toast.makeText(this,
                    "Montant superieur au reste a payer: " + String.format(Locale.US, "%.0f DH", remaining),
                    Toast.LENGTH_LONG).show();
            return null;
        }

        return new PaymentInput(reservation, montant, datePaiement);
    }

    private void savePaymentRecord(Reservation reservation, double montant, String datePaiement, String toastMessage) {
        boolean success = databaseHelper.addPayment(reservation.getId(), montant, datePaiement) != -1;
        if (success) {
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
            showPaymentNotification(reservation, montant);
            finish();
        } else {
            Toast.makeText(this, "Erreur d'enregistrement", Toast.LENGTH_SHORT).show();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                PAYMENT_CHANNEL_ID,
                "Paiements DriveFlow",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Notifications apres enregistrement des paiements");

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

    private void showPaymentNotification(Reservation reservation, double montant) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(this, PaymentListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String amount = String.format(Locale.US, "%.0f DH", montant);
        String message = reservation.toString() + " - " + amount;

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, PAYMENT_CHANNEL_ID)
                : new Notification.Builder(this);

        Notification notification = builder
                .setSmallIcon(R.drawable.ic_payment)
                .setContentTitle("Paiement enregistre")
                .setContentText(amount)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), notification);
        }
    }

    private boolean isValidDate(String date) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            format.setLenient(false);
            format.parse(date);
            return true;
        } catch (ParseException exception) {
            return false;
        }
    }

    private static class PaymentInput {
        final Reservation reservation;
        final double montant;
        final String datePaiement;

        PaymentInput(Reservation reservation, double montant, String datePaiement) {
            this.reservation = reservation;
            this.montant = montant;
            this.datePaiement = datePaiement;
        }
    }
}
