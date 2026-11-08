package com.example.locationvoitures.model;

public class Reservation {
    private int id;
    private int clientId;
    private int voitureId;
    private String clientName;
    private String carName;
    private String dateDebut;
    private String dateFin;
    private double prixTotal;
    private String statut;
    private double montantPaye;

    public Reservation(int id, int clientId, int voitureId, String clientName, String carName,
                       String dateDebut, String dateFin, double prixTotal, String statut) {
        this(id, clientId, voitureId, clientName, carName, dateDebut, dateFin, prixTotal, statut, 0);
    }

    public Reservation(int id, int clientId, int voitureId, String clientName, String carName,
                       String dateDebut, String dateFin, double prixTotal, String statut, double montantPaye) {
        this.id = id;
        this.clientId = clientId;
        this.voitureId = voitureId;
        this.clientName = clientName;
        this.carName = carName;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.prixTotal = prixTotal;
        this.statut = statut;
        this.montantPaye = montantPaye;
    }

    public int getId() {
        return id;
    }

    public int getClientId() {
        return clientId;
    }

    public int getVoitureId() {
        return voitureId;
    }

    public String getDateDebut() {
        return dateDebut;
    }

    public String getDateFin() {
        return dateFin;
    }

    public double getPrixTotal() {
        return prixTotal;
    }

    public String getStatut() {
        return statut;
    }

    public double getMontantPaye() {
        return montantPaye;
    }

    public double getResteAPayer() {
        return Math.max(0, prixTotal - montantPaye);
    }

    public String getPaymentStatus() {
        if (montantPaye <= 0) {
            return "Non paye";
        }
        if (montantPaye >= prixTotal) {
            return "Paye";
        }
        return "Partiel";
    }

    @Override
    public String toString() {
        return clientName + " - " + carName + "\n" + dateDebut + " au " + dateFin
                + " - " + prixTotal + " DH - " + statut
                + "\nPaiement: " + getPaymentStatus() + " (" + montantPaye + "/" + prixTotal + " DH)";
    }
}
