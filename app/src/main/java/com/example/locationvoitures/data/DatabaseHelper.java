package com.example.locationvoitures.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.locationvoitures.model.Car;
import com.example.locationvoitures.model.Client;
import com.example.locationvoitures.model.Payment;
import com.example.locationvoitures.model.Reservation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "location_voitures.db";
    private static final int DATABASE_VERSION = 6;
    private static final String PRELOADED_DATABASE_PATH = "preloaded/location_voitures.db";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        copyPreloadedDatabaseIfMissing(context.getApplicationContext());
    }

    private void copyPreloadedDatabaseIfMissing(Context context) {
        File databaseFile = context.getDatabasePath(DATABASE_NAME);
        if (databaseFile.exists()) {
            return;
        }

        File parent = databaseFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return;
        }

        try (InputStream input = context.getAssets().open(PRELOADED_DATABASE_PATH);
             FileOutputStream output = new FileOutputStream(databaseFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            output.flush();
        } catch (IOException exception) {
            if (databaseFile.exists()) {
                databaseFile.delete();
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE utilisateur (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password_hash TEXT NOT NULL, " +
                "role TEXT NOT NULL)");

        db.execSQL("CREATE TABLE voiture (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "marque TEXT NOT NULL, " +
                "modele TEXT NOT NULL, " +
                "annee INTEGER NOT NULL, " +
                "prix_jour REAL NOT NULL, " +
                "disponible INTEGER NOT NULL DEFAULT 1, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "image_uri TEXT)");

        db.execSQL("CREATE TABLE client (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nom TEXT NOT NULL, " +
                "prenom TEXT NOT NULL, " +
                "email TEXT NOT NULL, " +
                "telephone TEXT NOT NULL, " +
                "user_id INTEGER, " +
                "FOREIGN KEY(user_id) REFERENCES utilisateur(id))");

        db.execSQL("CREATE TABLE reservation (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "client_id INTEGER NOT NULL, " +
                "voiture_id INTEGER NOT NULL, " +
                "date_debut TEXT NOT NULL, " +
                "date_fin TEXT NOT NULL, " +
                "prix_total REAL NOT NULL, " +
                "statut TEXT NOT NULL, " +
                "FOREIGN KEY(client_id) REFERENCES client(id), " +
                "FOREIGN KEY(voiture_id) REFERENCES voiture(id))");

        db.execSQL("CREATE TABLE paiement (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "reservation_id INTEGER NOT NULL, " +
                "montant REAL NOT NULL, " +
                "date_paiement TEXT NOT NULL, " +
                "FOREIGN KEY(reservation_id) REFERENCES reservation(id))");

        insertDefaultAdmin(db);
        insertDefaultEmployee(db);
        insertDemoDataIfEmpty(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            addColumnIfMissing(db, "ALTER TABLE voiture ADD COLUMN latitude REAL");
            addColumnIfMissing(db, "ALTER TABLE voiture ADD COLUMN longitude REAL");
        }
        if (oldVersion < 3) {
            addColumnIfMissing(db, "ALTER TABLE client ADD COLUMN user_id INTEGER");
        }
        if (oldVersion < 4) {
            addColumnIfMissing(db, "ALTER TABLE voiture ADD COLUMN image_uri TEXT");
        }
        if (oldVersion < 5) {
            insertDefaultEmployee(db);
        }
        if (oldVersion < 6) {
            insertDemoDataIfEmpty(db);
        }
    }

    private void addColumnIfMissing(SQLiteDatabase db, String sql) {
        try {
            db.execSQL(sql);
        } catch (SQLException ignored) {
            // Column already exists on this local database.
        }
    }

    private void insertDefaultAdmin(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put("username", "admin");
        values.put("password_hash", hashPassword("admin123"));
        values.put("role", "Administrateur");
        db.insertWithOnConflict("utilisateur", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void insertDefaultEmployee(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put("username", "employe");
        values.put("password_hash", hashPassword("employe123"));
        values.put("role", "Employe");
        db.insertWithOnConflict("utilisateur", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void insertDemoDataIfEmpty(SQLiteDatabase db) {
        boolean hasBusinessData = countRows(db, "voiture") > 0
                || countRows(db, "client") > 0
                || countRows(db, "reservation") > 0
                || countRows(db, "paiement") > 0;
        if (hasBusinessData) {
            return;
        }

        ContentValues userValues = new ContentValues();
        userValues.put("username", "client");
        userValues.put("password_hash", hashPassword("client123"));
        userValues.put("role", "Client");
        long clientUserId = db.insertWithOnConflict(
                "utilisateur",
                null,
                userValues,
                SQLiteDatabase.CONFLICT_IGNORE
        );
        if (clientUserId == -1) {
            clientUserId = getUserId(db, "client");
        }

        int clientA = insertDemoClient(db, "Benali", "Yassine", "client.a@example.com", "0600000001", (int) clientUserId);
        int clientB = insertDemoClient(db, "El Mansouri", "Salma", "client.b@example.com", "0600000002", null);
        int clientC = insertDemoClient(db, "Ait Omar", "Karim", "client.c@example.com", "0600000003", null);

        int carA = insertDemoCar(db, "Toyota", "Corolla", 2022, 450, true, 33.5731, -7.5898);
        int carB = insertDemoCar(db, "Dacia", "Duster", 2021, 520, false, 33.5899, -7.6039);
        int carC = insertDemoCar(db, "Hyundai", "Tucson", 2023, 650, true, 33.5951, -7.6188);
        int carD = insertDemoCar(db, "Renault", "Clio", 2020, 300, true, 33.5650, -7.6114);

        int reservationA = insertDemoReservation(db, clientA, carA, "2026-05-15", "2026-05-18", 1350, "En attente");
        int reservationB = insertDemoReservation(db, clientB, carB, "2026-05-08", "2026-05-12", 2080, "Confirmee");
        insertDemoReservation(db, clientC, carC, "2026-05-20", "2026-05-22", 1300, "Annulee");

        if (reservationB != -1) {
            insertDemoPayment(db, reservationB, 1000, "2026-05-09");
        }
        if (reservationA != -1) {
            // Reservation intentionally left pending to demonstrate admin/employee validation.
        }
        if (carD != -1) {
            // Extra available vehicle for the client reservation scenario.
        }
    }

    private int countRows(SQLiteDatabase db, String table) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + table, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    private long getUserId(SQLiteDatabase db, String username) {
        Cursor cursor = db.rawQuery("SELECT id FROM utilisateur WHERE username = ?", new String[]{username});
        long userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getLong(0);
        }
        cursor.close();
        return userId;
    }

    private int insertDemoClient(SQLiteDatabase db, String nom, String prenom, String email,
                                 String telephone, Integer userId) {
        ContentValues values = new ContentValues();
        values.put("nom", nom);
        values.put("prenom", prenom);
        values.put("email", email);
        values.put("telephone", telephone);
        if (userId != null && userId > 0) {
            values.put("user_id", userId);
        }
        return (int) db.insert("client", null, values);
    }

    private int insertDemoCar(SQLiteDatabase db, String marque, String modele, int annee,
                              double prixJour, boolean disponible, double latitude, double longitude) {
        ContentValues values = new ContentValues();
        values.put("marque", marque);
        values.put("modele", modele);
        values.put("annee", annee);
        values.put("prix_jour", prixJour);
        values.put("disponible", disponible ? 1 : 0);
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        return (int) db.insert("voiture", null, values);
    }

    private int insertDemoReservation(SQLiteDatabase db, int clientId, int voitureId,
                                      String dateDebut, String dateFin, double prixTotal, String statut) {
        if (clientId == -1 || voitureId == -1) {
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put("client_id", clientId);
        values.put("voiture_id", voitureId);
        values.put("date_debut", dateDebut);
        values.put("date_fin", dateFin);
        values.put("prix_total", prixTotal);
        values.put("statut", statut);
        return (int) db.insert("reservation", null, values);
    }

    private void insertDemoPayment(SQLiteDatabase db, int reservationId, double montant, String datePaiement) {
        ContentValues values = new ContentValues();
        values.put("reservation_id", reservationId);
        values.put("montant", montant);
        values.put("date_paiement", datePaiement);
        db.insert("paiement", null, values);
    }

    public boolean registerUser(String username, String password, String role) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username.trim());
        values.put("password_hash", hashPassword(password));
        values.put("role", role);
        long userId = db.insert("utilisateur", null, values);
        if (userId == -1) {
            return false;
        }
        if ("Client".equals(role)) {
            createClientForUser((int) userId, username.trim());
        }
        return true;
    }

    private void createClientForUser(int userId, String username) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nom", username);
        values.put("prenom", "Client");
        values.put("email", username + "@driveflow.local");
        values.put("telephone", "A renseigner");
        values.put("user_id", userId);
        db.insert("client", null, values);
    }

    public boolean isValidLogin(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM utilisateur WHERE username = ? AND password_hash = ?",
                new String[]{username.trim(), hashPassword(password)}
        );
        boolean valid = cursor.moveToFirst();
        cursor.close();
        return valid;
    }

    public String getUserRole(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT role FROM utilisateur WHERE username = ?",
                new String[]{username.trim()}
        );
        String role = "Client";
        if (cursor.moveToFirst()) {
            role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
        }
        cursor.close();
        return role;
    }

    private int getUserId(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM utilisateur WHERE username = ?",
                new String[]{username == null ? "" : username.trim()}
        );
        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        }
        cursor.close();
        return userId;
    }

    public long addCar(String marque, String modele, int annee, double prixJour, boolean disponible) {
        return addCar(marque, modele, annee, prixJour, disponible, null, null);
    }

    public long addCar(String marque, String modele, int annee, double prixJour, boolean disponible,
                       Double latitude, Double longitude) {
        return addCar(marque, modele, annee, prixJour, disponible, latitude, longitude, null);
    }

    public long addCar(String marque, String modele, int annee, double prixJour, boolean disponible,
                       Double latitude, Double longitude, String imageUri) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = carValues(marque, modele, annee, prixJour, disponible, latitude, longitude, imageUri);
        return db.insert("voiture", null, values);
    }

    public boolean updateCar(int id, String marque, String modele, int annee, double prixJour, boolean disponible) {
        return updateCar(id, marque, modele, annee, prixJour, disponible, null, null);
    }

    public boolean updateCar(int id, String marque, String modele, int annee, double prixJour, boolean disponible,
                             Double latitude, Double longitude) {
        return updateCar(id, marque, modele, annee, prixJour, disponible, latitude, longitude, null);
    }

    public boolean updateCar(int id, String marque, String modele, int annee, double prixJour, boolean disponible,
                             Double latitude, Double longitude, String imageUri) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = carValues(marque, modele, annee, prixJour, disponible, latitude, longitude, imageUri);
        return db.update("voiture", values, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteCar(int id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete("voiture", "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    /** A client with reservations must not be deleted: its reservations and payments would become orphans. */
    public boolean clientHasReservations(int clientId) {
        return exists("SELECT id FROM reservation WHERE client_id = ? LIMIT 1", clientId);
    }

    /** A reservation with payments must not be deleted: the payments would vanish from the lists but stay counted. */
    public boolean reservationHasPayments(int reservationId) {
        return exists("SELECT id FROM paiement WHERE reservation_id = ? LIMIT 1", reservationId);
    }

    private boolean exists(String sql, int id) {
        Cursor cursor = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(id)});
        boolean found = cursor.moveToFirst();
        cursor.close();
        return found;
    }

    public boolean carHasReservations(int carId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM reservation WHERE voiture_id = ? LIMIT 1",
                new String[]{String.valueOf(carId)}
        );
        boolean hasReservations = cursor.moveToFirst();
        cursor.close();
        return hasReservations;
    }

    public Car getCarById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM voiture WHERE id = ?", new String[]{String.valueOf(id)});
        Car car = null;
        if (cursor.moveToFirst()) {
            car = readCar(cursor);
        }
        cursor.close();
        return car;
    }

    public ArrayList<Car> getCars() {
        ArrayList<Car> cars = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM voiture ORDER BY marque, modele", null);
        while (cursor.moveToNext()) {
            cars.add(readCar(cursor));
        }
        cursor.close();
        return cars;
    }

    public long addClient(String nom, String prenom, String email, String telephone) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = clientValues(nom, prenom, email, telephone);
        return db.insert("client", null, values);
    }

    public boolean updateClient(int id, String nom, String prenom, String email, String telephone) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = clientValues(nom, prenom, email, telephone);
        return db.update("client", values, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean isClientEmailUsed(String email, int exceptClientId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM client WHERE email = ? AND id != ? LIMIT 1",
                new String[]{email.trim(), String.valueOf(exceptClientId)}
        );
        boolean used = cursor.moveToFirst();
        cursor.close();
        return used;
    }

    public boolean deleteClient(int id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete("client", "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public Client getClientById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM client WHERE id = ?", new String[]{String.valueOf(id)});
        Client client = null;
        if (cursor.moveToFirst()) {
            client = readClient(cursor);
        }
        cursor.close();
        return client;
    }

    public Client getOrCreateClientForUser(String username) {
        String safeUsername = username == null || username.trim().isEmpty() ? "client" : username.trim();
        String generatedEmail = safeUsername + "@driveflow.local";
        int userId = getUserId(safeUsername);

        if (userId != -1) {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM client WHERE user_id = ?", new String[]{String.valueOf(userId)});
            Client linkedClient = null;
            if (cursor.moveToFirst()) {
                linkedClient = readClient(cursor);
            }
            cursor.close();
            if (linkedClient != null) {
                return linkedClient;
            }
        }

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM client WHERE email = ?", new String[]{generatedEmail});
        Client client = null;
        if (cursor.moveToFirst()) {
            client = readClient(cursor);
        }
        cursor.close();
        if (client != null) {
            if (userId != -1) {
                SQLiteDatabase writableDb = getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("user_id", userId);
                writableDb.update("client", values, "id = ?", new String[]{String.valueOf(client.getId())});
                return getClientById(client.getId());
            }
            return client;
        }

        if (userId != -1) {
            createClientForUser(userId, safeUsername);
            return getOrCreateClientForUser(safeUsername);
        }
        long id = addClient(safeUsername, "Client", generatedEmail, "A renseigner");
        return getClientById((int) id);
    }

    public ArrayList<Client> getClients() {
        ArrayList<Client> clients = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM client ORDER BY nom, prenom", null);
        while (cursor.moveToNext()) {
            clients.add(readClient(cursor));
        }
        cursor.close();
        return clients;
    }

    public long addReservation(int clientId, int voitureId, String dateDebut, String dateFin,
                               double prixTotal, String statut) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = reservationValues(clientId, voitureId, dateDebut, dateFin, prixTotal, statut);
        return db.insert("reservation", null, values);
    }

    public boolean updateReservation(int id, int clientId, int voitureId, String dateDebut, String dateFin,
                                     double prixTotal, String statut) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = reservationValues(clientId, voitureId, dateDebut, dateFin, prixTotal, statut);
        return db.update("reservation", values, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteReservation(int id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete("reservation", "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public Reservation getReservationById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(reservationQuery() + " WHERE r.id = ?", new String[]{String.valueOf(id)});
        Reservation reservation = null;
        if (cursor.moveToFirst()) {
            reservation = readReservation(cursor);
        }
        cursor.close();
        return reservation;
    }

    public ArrayList<Reservation> getReservations() {
        ArrayList<Reservation> reservations = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(reservationQuery() + " ORDER BY r.id DESC", null);
        while (cursor.moveToNext()) {
            reservations.add(readReservation(cursor));
        }
        cursor.close();
        return reservations;
    }

    public ArrayList<Reservation> getConfirmedReservationsWithBalance() {
        ArrayList<Reservation> reservations = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(reservationQuery()
                + " WHERE r.statut = 'Confirmee' "
                + "AND r.prix_total > IFNULL((SELECT SUM(p.montant) FROM paiement p WHERE p.reservation_id = r.id), 0) "
                + "ORDER BY r.id DESC", null);
        while (cursor.moveToNext()) {
            reservations.add(readReservation(cursor));
        }
        cursor.close();
        return reservations;
    }

    public ArrayList<Reservation> getReservationsForClient(int clientId) {
        ArrayList<Reservation> reservations = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(reservationQuery() + " WHERE r.client_id = ? ORDER BY r.id DESC",
                new String[]{String.valueOf(clientId)});
        while (cursor.moveToNext()) {
            reservations.add(readReservation(cursor));
        }
        cursor.close();
        return reservations;
    }

    public long addPayment(int reservationId, double montant, String datePaiement) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("reservation_id", reservationId);
        values.put("montant", montant);
        values.put("date_paiement", datePaiement.trim());
        return db.insert("paiement", null, values);
    }

    public boolean deletePayment(int id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete("paiement", "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public ArrayList<Payment> getPayments() {
        ArrayList<Payment> payments = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT p.id, p.reservation_id, p.montant, p.date_paiement, " +
                "c.prenom || ' ' || c.nom || ' - ' || v.marque || ' ' || v.modele AS reservation_label " +
                "FROM paiement p " +
                "JOIN reservation r ON p.reservation_id = r.id " +
                "JOIN client c ON r.client_id = c.id " +
                "JOIN voiture v ON r.voiture_id = v.id " +
                "ORDER BY p.id DESC", null);
        while (cursor.moveToNext()) {
            payments.add(new Payment(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("reservation_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("reservation_label")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("montant")),
                    cursor.getString(cursor.getColumnIndexOrThrow("date_paiement"))
            ));
        }
        cursor.close();
        return payments;
    }

    public int countRows(String table) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + table, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int countAvailableCars() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM voiture WHERE disponible = 1", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int countReservationsByStatus(String status) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM reservation WHERE statut = ?", new String[]{status});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public double getTotalPaidForReservation(int reservationId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT IFNULL(SUM(montant), 0) FROM paiement WHERE reservation_id = ?",
                new String[]{String.valueOf(reservationId)}
        );
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public double getRemainingForReservation(Reservation reservation) {
        return Math.max(0, reservation.getPrixTotal() - getTotalPaidForReservation(reservation.getId()));
    }

    public boolean hasConfirmedReservationOverlap(int carId, String dateDebut, String dateFin, int excludeReservationId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM reservation " +
                        "WHERE voiture_id = ? " +
                        "AND statut = 'Confirmee' " +
                        "AND id != ? " +
                        "AND date_debut < ? " +
                        "AND date_fin > ? " +
                        "LIMIT 1",
                new String[]{
                        String.valueOf(carId),
                        String.valueOf(excludeReservationId),
                        dateFin,
                        dateDebut
                }
        );
        boolean overlap = cursor.moveToFirst();
        cursor.close();
        return overlap;
    }

    private void updateCarAvailability(int carId, boolean disponible) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("disponible", disponible ? 1 : 0);
        db.update("voiture", values, "id = ?", new String[]{String.valueOf(carId)});
    }

    private ContentValues carValues(String marque, String modele, int annee, double prixJour, boolean disponible) {
        return carValues(marque, modele, annee, prixJour, disponible, null, null);
    }

    private ContentValues carValues(String marque, String modele, int annee, double prixJour, boolean disponible,
                                    Double latitude, Double longitude) {
        return carValues(marque, modele, annee, prixJour, disponible, latitude, longitude, null);
    }

    private ContentValues carValues(String marque, String modele, int annee, double prixJour, boolean disponible,
                                    Double latitude, Double longitude, String imageUri) {
        ContentValues values = new ContentValues();
        values.put("marque", marque.trim());
        values.put("modele", modele.trim());
        values.put("annee", annee);
        values.put("prix_jour", prixJour);
        values.put("disponible", disponible ? 1 : 0);
        if (latitude != null && longitude != null) {
            values.put("latitude", latitude);
            values.put("longitude", longitude);
        } else {
            values.putNull("latitude");
            values.putNull("longitude");
        }
        if (imageUri != null && !imageUri.trim().isEmpty()) {
            values.put("image_uri", imageUri.trim());
        } else {
            values.putNull("image_uri");
        }
        return values;
    }

    private ContentValues clientValues(String nom, String prenom, String email, String telephone) {
        ContentValues values = new ContentValues();
        values.put("nom", nom.trim());
        values.put("prenom", prenom.trim());
        values.put("email", email.trim());
        values.put("telephone", telephone.trim());
        return values;
    }

    private ContentValues reservationValues(int clientId, int voitureId, String dateDebut, String dateFin,
                                            double prixTotal, String statut) {
        ContentValues values = new ContentValues();
        values.put("client_id", clientId);
        values.put("voiture_id", voitureId);
        values.put("date_debut", dateDebut.trim());
        values.put("date_fin", dateFin.trim());
        values.put("prix_total", prixTotal);
        values.put("statut", statut);
        return values;
    }

    private String reservationQuery() {
        return "SELECT r.id, r.client_id, r.voiture_id, r.date_debut, r.date_fin, r.prix_total, r.statut, " +
                "IFNULL((SELECT SUM(p.montant) FROM paiement p WHERE p.reservation_id = r.id), 0) AS montant_paye, " +
                "c.prenom || ' ' || c.nom AS client_name, v.marque || ' ' || v.modele AS car_name " +
                "FROM reservation r " +
                "JOIN client c ON r.client_id = c.id " +
                "JOIN voiture v ON r.voiture_id = v.id";
    }

    private Car readCar(Cursor cursor) {
        return new Car(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("marque")),
                cursor.getString(cursor.getColumnIndexOrThrow("modele")),
                cursor.getInt(cursor.getColumnIndexOrThrow("annee")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("prix_jour")),
                cursor.getInt(cursor.getColumnIndexOrThrow("disponible")) == 1,
                cursor.isNull(cursor.getColumnIndexOrThrow("latitude"))
                        ? null
                        : cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                cursor.isNull(cursor.getColumnIndexOrThrow("longitude"))
                        ? null
                        : cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                cursor.isNull(cursor.getColumnIndexOrThrow("image_uri"))
                        ? null
                        : cursor.getString(cursor.getColumnIndexOrThrow("image_uri"))
        );
    }

    private Client readClient(Cursor cursor) {
        return new Client(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("nom")),
                cursor.getString(cursor.getColumnIndexOrThrow("prenom")),
                cursor.getString(cursor.getColumnIndexOrThrow("email")),
                cursor.getString(cursor.getColumnIndexOrThrow("telephone"))
        );
    }

    private Reservation readReservation(Cursor cursor) {
        return new Reservation(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getInt(cursor.getColumnIndexOrThrow("client_id")),
                cursor.getInt(cursor.getColumnIndexOrThrow("voiture_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("client_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("car_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("date_debut")),
                cursor.getString(cursor.getColumnIndexOrThrow("date_fin")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("prix_total")),
                cursor.getString(cursor.getColumnIndexOrThrow("statut")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("montant_paye"))
        );
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes());
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
