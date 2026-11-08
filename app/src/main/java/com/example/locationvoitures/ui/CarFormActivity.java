package com.example.locationvoitures.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.locationvoitures.R;
import com.example.locationvoitures.data.DatabaseHelper;
import com.example.locationvoitures.model.Car;

import java.util.Calendar;

public class CarFormActivity extends Activity {
    private static final int LOCATION_REQUEST_CODE = 42;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 79;
    private static final int IMAGE_PICK_REQUEST_CODE = 80;

    private EditText editMarque;
    private EditText editModele;
    private EditText editAnnee;
    private EditText editPrixJour;
    private EditText editLatitude;
    private EditText editLongitude;
    private ImageView imageCarPreview;
    private CheckBox checkDisponible;
    private DatabaseHelper databaseHelper;
    private int carId = -1;
    private Boolean previousDisponible;
    private String selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_form);

        databaseHelper = new DatabaseHelper(this);
        carId = getIntent().getIntExtra("car_id", -1);

        TextView textTitle = findViewById(R.id.textTitle);
        editMarque = findViewById(R.id.editMarque);
        editModele = findViewById(R.id.editModele);
        editAnnee = findViewById(R.id.editAnnee);
        editPrixJour = findViewById(R.id.editPrixJour);
        editLatitude = findViewById(R.id.editLatitude);
        editLongitude = findViewById(R.id.editLongitude);
        imageCarPreview = findViewById(R.id.imageCarPreview);
        checkDisponible = findViewById(R.id.checkDisponible);
        Button buttonChooseImage = findViewById(R.id.buttonChooseImage);
        Button buttonRemoveImage = findViewById(R.id.buttonRemoveImage);
        Button buttonUseLocation = findViewById(R.id.buttonUseLocation);
        Button buttonOpenMap = findViewById(R.id.buttonOpenMap);
        Button buttonSave = findViewById(R.id.buttonSave);

        requestNotificationPermissionIfNeeded();
        textTitle.setText(carId == -1 ? "Ajouter une voiture" : "Modifier une voiture");
        if (carId != -1) {
            loadCar();
        }

        findViewById(R.id.buttonBack).setOnClickListener(view -> finish());
        buttonChooseImage.setOnClickListener(view -> chooseImage());
        buttonRemoveImage.setOnClickListener(view -> removeImage());
        buttonUseLocation.setOnClickListener(view -> captureCurrentLocation());
        buttonOpenMap.setOnClickListener(view -> openMap());
        buttonSave.setOnClickListener(view -> saveCar());
    }

    private void loadCar() {
        Car car = databaseHelper.getCarById(carId);
        if (car == null) {
            Toast.makeText(this, "Voiture introuvable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editMarque.setText(car.getMarque());
        editModele.setText(car.getModele());
        editAnnee.setText(String.valueOf(car.getAnnee()));
        editPrixJour.setText(String.valueOf(car.getPrixJour()));
        checkDisponible.setChecked(car.isDisponible());
        previousDisponible = car.isDisponible();
        if (car.hasImage()) {
            selectedImageUri = car.getImageUri();
            showSelectedImage();
        }
        if (car.hasLocation()) {
            editLatitude.setText(String.valueOf(car.getLatitude()));
            editLongitude.setText(String.valueOf(car.getLongitude()));
        }
    }

    private void saveCar() {
        String marque = editMarque.getText().toString().trim();
        String modele = editModele.getText().toString().trim();
        String anneeText = editAnnee.getText().toString().trim();
        String prixText = editPrixJour.getText().toString().trim();
        String latitudeText = editLatitude.getText().toString().trim();
        String longitudeText = editLongitude.getText().toString().trim();

        if (marque.isEmpty() || modele.isEmpty() || anneeText.isEmpty() || prixText.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }
        if (marque.length() < 2 || modele.length() < 1) {
            Toast.makeText(this, "Marque ou modele trop court", Toast.LENGTH_SHORT).show();
            return;
        }
        if (latitudeText.isEmpty() != longitudeText.isEmpty()) {
            Toast.makeText(this, "Latitude et longitude doivent etre remplies ensemble", Toast.LENGTH_SHORT).show();
            return;
        }

        int annee;
        double prixJour;
        Double latitude = null;
        Double longitude = null;
        try {
            annee = Integer.parseInt(anneeText);
            prixJour = Double.parseDouble(prixText);
            if (!latitudeText.isEmpty() || !longitudeText.isEmpty()) {
                latitude = Double.parseDouble(latitudeText);
                longitude = Double.parseDouble(longitudeText);
            }
        } catch (NumberFormatException exception) {
            Toast.makeText(this, "Annee ou prix invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        int maxYear = Calendar.getInstance().get(Calendar.YEAR) + 1;
        if (annee < 1990 || annee > maxYear || prixJour <= 0) {
            Toast.makeText(this, "Annee ou prix invalide", Toast.LENGTH_SHORT).show();
            return;
        }
        if (latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
            Toast.makeText(this, "Coordonnees GPS invalides", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean disponible = checkDisponible.isChecked();
        boolean success;
        if (carId == -1) {
            success = databaseHelper.addCar(marque, modele, annee, prixJour, disponible, latitude, longitude, selectedImageUri) != -1;
        } else {
            success = databaseHelper.updateCar(carId, marque, modele, annee, prixJour, disponible, latitude, longitude, selectedImageUri);
        }

        if (success) {
            Toast.makeText(this, "Voiture enregistree", Toast.LENGTH_SHORT).show();
            if (carId == -1 || previousDisponible == null || previousDisponible != disponible) {
                NotificationUtils.notifyCarAvailability(this, marque + " " + modele, disponible);
            }
            finish();
        } else {
            Toast.makeText(this, "Erreur d'enregistrement", Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, IMAGE_PICK_REQUEST_CODE);
    }

    private void removeImage() {
        selectedImageUri = null;
        imageCarPreview.setImageTintList(null);
        imageCarPreview.setImageResource(R.drawable.ic_car);
        imageCarPreview.setColorFilter(getColor(R.color.primaryBlue));
        imageCarPreview.setPadding(18, 18, 18, 18);
    }

    private void showSelectedImage() {
        if (selectedImageUri == null || selectedImageUri.trim().isEmpty()) {
            removeImage();
            return;
        }
        try {
            imageCarPreview.setImageTintList(null);
            imageCarPreview.clearColorFilter();
            imageCarPreview.setPadding(0, 0, 0, 0);
            imageCarPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageCarPreview.setImageURI(null);
            imageCarPreview.setImageURI(Uri.parse(selectedImageUri));
        } catch (Exception exception) {
            Toast.makeText(this, "Impossible d'afficher cette image", Toast.LENGTH_SHORT).show();
            removeImage();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void captureCurrentLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = null;
        if (locationManager != null) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }

        if (location == null) {
            Toast.makeText(this, "Position indisponible. Activez le GPS puis reessayez.", Toast.LENGTH_LONG).show();
            return;
        }

        editLatitude.setText(String.format(java.util.Locale.US, "%.6f", location.getLatitude()));
        editLongitude.setText(String.format(java.util.Locale.US, "%.6f", location.getLongitude()));
        Toast.makeText(this, "Position ajoutee", Toast.LENGTH_SHORT).show();
    }

    private void openMap() {
        String latitudeText = editLatitude.getText().toString().trim();
        String longitudeText = editLongitude.getText().toString().trim();
        if (latitudeText.isEmpty() || longitudeText.isEmpty()) {
            Toast.makeText(this, "Ajoutez d'abord une position GPS", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.parse("geo:" + latitudeText + "," + longitudeText
                + "?q=" + latitudeText + "," + longitudeText + "(Voiture DriveFlow)");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(Intent.createChooser(intent, "Ouvrir la position"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            captureCurrentLocation();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(
                        imageUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (SecurityException ignored) {
                // Some providers grant temporary read access only.
            }
            selectedImageUri = imageUri.toString();
            showSelectedImage();
        }
    }
}
