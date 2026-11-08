package com.example.locationvoitures.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.example.locationvoitures.R;
import com.example.locationvoitures.data.DatabaseHelper;
import com.example.locationvoitures.model.Car;

import java.util.ArrayList;

public class CarListActivity extends Activity {
    private DatabaseHelper databaseHelper;
    private ArrayList<Car> cars;
    private ArrayAdapter<Car> adapter;
    private boolean canManage;
    private boolean canDelete;
    private String role;
    private String username;
    private String activeFilter = "all";
    private String searchQuery = "";
    private TextView textSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        databaseHelper = new DatabaseHelper(this);
        role = getIntent().getStringExtra("role");
        username = getIntent().getStringExtra("username");
        canManage = !"Client".equals(role);
        canDelete = "Administrateur".equals(role);
        TextView textTitle = findViewById(R.id.textTitle);
        textSubtitle = findViewById(R.id.textSubtitle);
        TextView chipAll = findViewById(R.id.chipAll);
        TextView chipDisponibles = findViewById(R.id.chipDisponibles);
        TextView chipRecents = findViewById(R.id.chipRecents);
        EditText editSearch = findViewById(R.id.editSearch);
        Button buttonAdd = findViewById(R.id.buttonAdd);
        Button buttonMap = findViewById(R.id.buttonMap);
        ListView listItems = findViewById(R.id.listItems);

        textTitle.setText("Voitures");
        buttonAdd.setText("Ajouter une voiture");
        if (!canManage) {
            buttonAdd.setText("Carte des voitures");
            textTitle.setText("Voitures disponibles");
        } else {
            buttonMap.setVisibility(View.VISIBLE);
        }

        cars = new ArrayList<>();
        adapter = new CarAdapter();
        listItems.setAdapter(adapter);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                loadCars();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        buttonAdd.setOnClickListener(view -> {
            if (canManage) {
                startActivity(new Intent(this, CarFormActivity.class));
            } else {
                openCarMapScreen();
            }
        });
        buttonMap.setOnClickListener(view -> openCarMapScreen());
        chipAll.setOnClickListener(view -> {
            activeFilter = "all";
            setActiveChip(chipAll, chipDisponibles, chipRecents);
            loadCars();
        });
        chipDisponibles.setOnClickListener(view -> {
            activeFilter = "available";
            setActiveChip(chipDisponibles, chipAll, chipRecents);
            loadCars();
        });
        chipRecents.setOnClickListener(view -> {
            activeFilter = "recent";
            setActiveChip(chipRecents, chipAll, chipDisponibles);
            loadCars();
        });
        listItems.setOnItemClickListener((parent, view, position, id) -> {
            if (!canManage) {
                showClientCarActions(cars.get(position));
                return;
            }
            Intent intent = new Intent(this, CarFormActivity.class);
            intent.putExtra("car_id", cars.get(position).getId());
            startActivity(intent);
        });
        listItems.setOnItemLongClickListener((parent, view, position, id) -> {
            if (!canDelete) {
                if (canManage) {
                    Toast.makeText(this, "Suppression reservee a l'administrateur", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            confirmDelete(cars.get(position));
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCars();
    }

    private void loadCars() {
        cars.clear();
        for (Car car : databaseHelper.getCars()) {
            if (shouldShowCar(car)) {
                cars.add(car);
            }
        }
        if ("recent".equals(activeFilter)) {
            cars.sort((first, second) -> Integer.compare(second.getId(), first.getId()));
        }
        updateSubtitle();
        adapter.notifyDataSetChanged();
    }

    private boolean shouldShowCar(Car car) {
        if (!canManage && !car.isDisponible()) {
            return false;
        }
        if ("available".equals(activeFilter) && !car.isDisponible()) {
            return false;
        }
        if (searchQuery.isEmpty()) {
            return true;
        }
        String haystack = (car.getMarque() + " " + car.getModele() + " " + car.getAnnee()).toLowerCase();
        return haystack.contains(searchQuery);
    }

    private void updateSubtitle() {
        int availableCount = 0;
        for (Car car : databaseHelper.getCars()) {
            if (car.isDisponible()) {
                availableCount++;
            }
        }
        textSubtitle.setText(cars.size() + " affichee(s) - " + availableCount + " disponible(s)");
    }

    private void setActiveChip(TextView active, TextView firstInactive, TextView secondInactive) {
        active.setBackgroundResource(R.drawable.bg_chip_active);
        active.setTextColor(getColor(android.R.color.white));
        firstInactive.setBackgroundResource(R.drawable.bg_chip_inactive);
        firstInactive.setTextColor(getColor(R.color.textSecondary));
        secondInactive.setBackgroundResource(R.drawable.bg_chip_inactive);
        secondInactive.setTextColor(getColor(R.color.textSecondary));
    }

    private void openCarMap(Car car) {
        if (!car.hasLocation()) {
            Toast.makeText(this, "Position non renseignee pour cette voiture", Toast.LENGTH_SHORT).show();
            return;
        }

        String latitude = String.valueOf(car.getLatitude());
        String longitude = String.valueOf(car.getLongitude());
        Uri uri = Uri.parse("geo:" + latitude + "," + longitude
                + "?q=" + latitude + "," + longitude + "(" + car.getMarque() + " " + car.getModele() + ")");
        try {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, uri), "Ouvrir la position"));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, "Aucune application carte installee", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCarMapScreen() {
        Intent intent = new Intent(this, CarMapActivity.class);
        intent.putExtra("role", role);
        intent.putExtra("username", username);
        startActivity(intent);
    }

    private void openAvailableCarsMap() {
        Car firstLocatedCar = null;
        int locatedCount = 0;
        for (Car car : cars) {
            if (car.hasLocation()) {
                locatedCount++;
                if (firstLocatedCar == null) {
                    firstLocatedCar = car;
                }
            }
        }

        if (firstLocatedCar == null) {
            Toast.makeText(this, "Aucune voiture disponible avec position GPS", Toast.LENGTH_SHORT).show();
            return;
        }
        if (locatedCount == 1) {
            openCarMap(firstLocatedCar);
            return;
        }

        Uri uri = Uri.parse("geo:" + firstLocatedCar.getLatitude() + "," + firstLocatedCar.getLongitude()
                + "?q=DriveFlow voitures disponibles");
        try {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, uri), "Ouvrir la carte"));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, "Aucune application carte installee", Toast.LENGTH_SHORT).show();
        }
    }

    private void showClientCarActions(Car car) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(car.getMarque() + " " + car.getModele())
                .setMessage("Prix: " + car.getPrixJour() + " DH/jour\nStatut: Disponible")
                .setPositiveButton("Reserver", (dialog, which) -> openReservationRequest(car));

        if (car.hasLocation()) {
            builder.setNeutralButton("Voir carte", (dialog, which) -> openCarMap(car));
        }
        builder.setNegativeButton("Fermer", null).show();
    }

    private void openReservationRequest(Car car) {
        Intent intent = new Intent(this, ReservationFormActivity.class);
        intent.putExtra("role", role);
        intent.putExtra("username", username);
        intent.putExtra("car_id", car.getId());
        startActivity(intent);
    }

    private void confirmDelete(Car car) {
        if (databaseHelper.carHasReservations(car.getId())) {
            Toast.makeText(this, "Impossible: cette voiture a des reservations", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Supprimer")
                .setMessage("Supprimer " + car.getMarque() + " " + car.getModele() + " ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    databaseHelper.deleteCar(car.getId());
                    Toast.makeText(this, "Voiture supprimee", Toast.LENGTH_SHORT).show();
                    loadCars();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private class CarAdapter extends ArrayAdapter<Car> {
        CarAdapter() {
            super(CarListActivity.this, R.layout.item_car, cars);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(CarListActivity.this).inflate(R.layout.item_car, parent, false);
            }

            Car car = getItem(position);
            if (car == null) {
                return view;
            }

            ImageView imageCar = view.findViewById(R.id.imageCar);
            LinearLayout iconBox = view.findViewById(R.id.iconBox);
            TextView textCarName = view.findViewById(R.id.textCarName);
            TextView textCarDetails = view.findViewById(R.id.textCarDetails);
            TextView textCarPrice = view.findViewById(R.id.textCarPrice);
            TextView badgeAvailability = view.findViewById(R.id.badgeAvailability);
            TextView buttonReserveCar = view.findViewById(R.id.buttonReserveCar);
            TextView buttonDeleteCar = view.findViewById(R.id.buttonDeleteCar);

            textCarName.setText(car.getMarque() + " " + car.getModele());
            textCarDetails.setText(car.getAnnee() + (car.hasLocation() ? " - GPS disponible" : " - GPS non renseigne"));
            textCarPrice.setText(String.format(java.util.Locale.US, "%.0f DH / jour", car.getPrixJour()));

            if (car.hasImage()) {
                imageCar.setImageTintList(null);
                imageCar.clearColorFilter();
                imageCar.setPadding(0, 0, 0, 0);
                imageCar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageCar.setImageURI(null);
                imageCar.setImageURI(Uri.parse(car.getImageUri()));
            } else {
                imageCar.setImageTintList(null);
                imageCar.setImageResource(R.drawable.ic_car);
                imageCar.setColorFilter(getColor(R.color.primaryBlue));
                imageCar.setPadding(14, 14, 14, 14);
                imageCar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }

            if (car.isDisponible()) {
                iconBox.setBackgroundResource(R.drawable.bg_icon_box_blue);
                badgeAvailability.setBackgroundResource(R.drawable.bg_status_success);
                badgeAvailability.setTextColor(getColor(R.color.successText));
                badgeAvailability.setText("Disponible");
            } else {
                iconBox.setBackgroundResource(R.drawable.bg_icon_box_orange);
                badgeAvailability.setBackgroundResource(R.drawable.bg_status_danger);
                badgeAvailability.setTextColor(getColor(R.color.dangerText));
                badgeAvailability.setText("Indisponible");
            }

            if (!canManage && car.isDisponible()) {
                buttonReserveCar.setVisibility(View.VISIBLE);
                buttonReserveCar.setOnClickListener(reserveView -> openReservationRequest(car));
            } else {
                buttonReserveCar.setVisibility(View.GONE);
                buttonReserveCar.setOnClickListener(null);
            }

            if (canDelete) {
                buttonDeleteCar.setVisibility(View.VISIBLE);
                buttonDeleteCar.setOnClickListener(deleteView -> confirmDelete(car));
            } else {
                buttonDeleteCar.setVisibility(View.GONE);
                buttonDeleteCar.setOnClickListener(null);
            }

            return view;
        }
    }
}
