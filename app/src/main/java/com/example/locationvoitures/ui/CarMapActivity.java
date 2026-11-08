package com.example.locationvoitures.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
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

import java.util.ArrayList;
import java.util.Locale;

public class CarMapActivity extends Activity {
    private DatabaseHelper databaseHelper;
    private ArrayList<Car> locatedCars;
    private ArrayAdapter<Car> adapter;
    private boolean isClientRole;
    private TextView textMapSubtitle;
    private TextView textMapStats;
    private Button buttonOpenFirstMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_map);

        databaseHelper = new DatabaseHelper(this);
        String role = getIntent().getStringExtra("role");
        isClientRole = "Client".equals(role);

        TextView textMapTitle = findViewById(R.id.textMapTitle);
        textMapSubtitle = findViewById(R.id.textMapSubtitle);
        textMapStats = findViewById(R.id.textMapStats);
        buttonOpenFirstMap = findViewById(R.id.buttonOpenFirstMap);
        ListView listMapCars = findViewById(R.id.listMapCars);

        textMapTitle.setText(isClientRole ? "Carte voitures disponibles" : "Carte des voitures");
        locatedCars = new ArrayList<>();
        adapter = new CarLocationAdapter();
        listMapCars.setAdapter(adapter);

        buttonOpenFirstMap.setOnClickListener(view -> {
            if (locatedCars.isEmpty()) {
                Toast.makeText(this, "Aucune voiture avec position GPS", Toast.LENGTH_SHORT).show();
                return;
            }
            openCarMap(locatedCars.get(0));
        });
        listMapCars.setOnItemClickListener((parent, view, position, id) -> openCarMap(locatedCars.get(position)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLocatedCars();
    }

    private void loadLocatedCars() {
        locatedCars.clear();
        int totalVisibleCars = 0;
        for (Car car : databaseHelper.getCars()) {
            if (isClientRole && !car.isDisponible()) {
                continue;
            }
            totalVisibleCars++;
            if (car.hasLocation()) {
                locatedCars.add(car);
            }
        }

        textMapSubtitle.setText(locatedCars.size() + " voiture(s) avec GPS");
        textMapStats.setText("Voitures visibles: " + totalVisibleCars
                + "\nPositions GPS renseignees: " + locatedCars.size()
                + "\nPositions manquantes: " + Math.max(0, totalVisibleCars - locatedCars.size()));
        buttonOpenFirstMap.setEnabled(!locatedCars.isEmpty());
        adapter.notifyDataSetChanged();
    }

    private void openCarMap(Car car) {
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

    private class CarLocationAdapter extends ArrayAdapter<Car> {
        CarLocationAdapter() {
            super(CarMapActivity.this, R.layout.item_car_location, locatedCars);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(CarMapActivity.this).inflate(R.layout.item_car_location, parent, false);
            }

            Car car = getItem(position);
            TextView textLocationCarName = view.findViewById(R.id.textLocationCarName);
            TextView textLocationDetails = view.findViewById(R.id.textLocationDetails);
            TextView buttonOpenCarMap = view.findViewById(R.id.buttonOpenCarMap);

            if (car != null) {
                textLocationCarName.setText(car.getMarque() + " " + car.getModele());
                textLocationDetails.setText(String.format(
                        Locale.US,
                        "%.5f, %.5f - %.0f DH/jour - %s",
                        car.getLatitude(),
                        car.getLongitude(),
                        car.getPrixJour(),
                        car.isDisponible() ? "Disponible" : "Indisponible"
                ));
                buttonOpenCarMap.setOnClickListener(mapView -> openCarMap(car));
            } else {
                textLocationCarName.setText("");
                textLocationDetails.setText("");
                buttonOpenCarMap.setOnClickListener(null);
            }
            return view;
        }
    }
}
