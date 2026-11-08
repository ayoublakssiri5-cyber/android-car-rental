package com.example.locationvoitures.model;

public class Car {
    private int id;
    private String marque;
    private String modele;
    private int annee;
    private double prixJour;
    private boolean disponible;
    private Double latitude;
    private Double longitude;
    private String imageUri;

    public Car(int id, String marque, String modele, int annee, double prixJour, boolean disponible) {
        this(id, marque, modele, annee, prixJour, disponible, null, null);
    }

    public Car(int id, String marque, String modele, int annee, double prixJour, boolean disponible,
               Double latitude, Double longitude) {
        this(id, marque, modele, annee, prixJour, disponible, latitude, longitude, null);
    }

    public Car(int id, String marque, String modele, int annee, double prixJour, boolean disponible,
               Double latitude, Double longitude, String imageUri) {
        this.id = id;
        this.marque = marque;
        this.modele = modele;
        this.annee = annee;
        this.prixJour = prixJour;
        this.disponible = disponible;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUri = imageUri;
    }

    public int getId() {
        return id;
    }

    public String getMarque() {
        return marque;
    }

    public String getModele() {
        return modele;
    }

    public int getAnnee() {
        return annee;
    }

    public double getPrixJour() {
        return prixJour;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    public String getImageUri() {
        return imageUri;
    }

    public boolean hasImage() {
        return imageUri != null && !imageUri.trim().isEmpty();
    }

    @Override
    public String toString() {
        String statut = disponible ? "Disponible" : "Non disponible";
        String location = hasLocation()
                ? "\nPosition: " + String.format("%.5f, %.5f", latitude, longitude)
                : "\nPosition: non renseignee";
        String image = hasImage() ? "\nImage: oui" : "\nImage: non";
        return marque + " " + modele + " - " + annee + "\n" + prixJour + " DH/jour - " + statut + location + image;
    }
}
