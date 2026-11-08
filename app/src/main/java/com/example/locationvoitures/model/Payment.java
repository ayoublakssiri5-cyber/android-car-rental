package com.example.locationvoitures.model;

public class Payment {
    private int id;
    private int reservationId;
    private String reservationLabel;
    private double montant;
    private String datePaiement;

    public Payment(int id, int reservationId, String reservationLabel, double montant, String datePaiement) {
        this.id = id;
        this.reservationId = reservationId;
        this.reservationLabel = reservationLabel;
        this.montant = montant;
        this.datePaiement = datePaiement;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return reservationLabel + "\n" + montant + " DH - " + datePaiement;
    }
}
