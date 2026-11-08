package com.example.locationvoitures.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.locationvoitures.model.Reservation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * SQLite scenarios (Robolectric). Assets are not packaged in unit tests, so the helper creates the schema and the
 * fictitious demo data of DatabaseHelper.onCreate. Test dates are in 2030 to stay clear of the demo reservations.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DatabaseHelperTest {
    private DatabaseHelper db;
    private int car;
    private int client;

    @Before
    public void setUp() {
        RuntimeEnvironment.getApplication().deleteDatabase("location_voitures.db");
        db = new DatabaseHelper(RuntimeEnvironment.getApplication());
        car = (int) db.addCar("Peugeot", "208", 2024, 400, true);
        client = (int) db.addClient("Test", "Client", "test.client@example.com", "0600000000");
    }

    @After
    public void tearDown() {
        db.close();
    }

    private int reserve(String start, String end, String status) {
        long days = RentalRules.daysBetween(start, end);
        return (int) db.addReservation(client, car, start, end, RentalRules.totalPrice(days, 400), status);
    }

    @Test
    public void loginWorksWithTheRightPasswordOnly() {
        assertTrue(db.registerUser("nouveau", "secret1", "Client"));
        assertTrue(db.isValidLogin("nouveau", "secret1"));
        assertTrue(db.isValidLogin(" nouveau ", "secret1"));
        assertFalse(db.isValidLogin("nouveau", "secret2"));
        assertEquals("Client", db.getUserRole("nouveau"));
        assertFalse(db.registerUser("nouveau", "autre", "Client")); // username is unique
    }

    @Test
    public void overlappingConfirmedReservationIsDetected() {
        int confirmed = reserve("2030-06-10", "2030-06-15", "Confirmee");
        assertTrue(db.hasConfirmedReservationOverlap(car, "2030-06-12", "2030-06-20", -1));
        assertTrue(db.hasConfirmedReservationOverlap(car, "2030-06-01", "2030-06-11", -1));
        assertFalse(db.hasConfirmedReservationOverlap(car, "2030-06-15", "2030-06-18", -1)); // returns that day
        assertFalse(db.hasConfirmedReservationOverlap(car, "2030-06-12", "2030-06-14", confirmed)); // itself
    }

    @Test
    public void pendingOrCancelledReservationsDoNotBlockTheCar() {
        reserve("2030-07-10", "2030-07-15", "En attente");
        reserve("2030-07-10", "2030-07-15", "Annulee");
        assertFalse(db.hasConfirmedReservationOverlap(car, "2030-07-11", "2030-07-12", -1));
    }

    @Test
    public void partialPaymentsReduceTheRemainingAmount() {
        int id = reserve("2030-08-01", "2030-08-04", "Confirmee"); // 3 days x 400 = 1200
        db.addPayment(id, 500, "2030-08-01");
        Reservation reservation = db.getReservationById(id);
        assertEquals(1200, reservation.getPrixTotal(), 0.001);
        assertEquals(700, db.getRemainingForReservation(reservation), 0.001);
        db.addPayment(id, 700, "2030-08-02");
        assertEquals(0, db.getRemainingForReservation(db.getReservationById(id)), 0.001);
    }

    @Test
    public void carWithReservationsIsProtected() {
        assertFalse(db.carHasReservations(car));
        reserve("2030-09-01", "2030-09-03", "En attente");
        assertTrue(db.carHasReservations(car));
    }

    @Test
    public void clientWithReservationsIsProtected() {
        assertFalse(db.clientHasReservations(client));
        reserve("2030-09-01", "2030-09-03", "En attente");
        assertTrue(db.clientHasReservations(client));
    }

    @Test
    public void reservationWithPaymentsIsProtected() {
        int id = reserve("2030-10-01", "2030-10-03", "Confirmee");
        assertFalse(db.reservationHasPayments(id));
        db.addPayment(id, 100, "2030-10-01");
        assertTrue(db.reservationHasPayments(id));
    }

    @Test
    public void demoDataUsesFictitiousAccounts() {
        assertTrue(db.isValidLogin("admin", "admin123"));
        assertEquals("Administrateur", db.getUserRole("admin"));
        assertTrue(db.countRows("voiture") >= 4);
    }
}
