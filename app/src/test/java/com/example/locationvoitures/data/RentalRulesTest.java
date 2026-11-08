package com.example.locationvoitures.data;

import static org.junit.Assert.assertEquals;

import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RentalRulesTest {
    private TimeZone original;

    @Before
    public void setUp() {
        original = TimeZone.getDefault();
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(original);
    }

    @Test
    public void daysBetweenTwoDates() {
        assertEquals(3, RentalRules.daysBetween("2026-05-15", "2026-05-18"));
        assertEquals(0, RentalRules.daysBetween("2026-05-15", "2026-05-15"));
        assertEquals(-3, RentalRules.daysBetween("2026-05-18", "2026-05-15"));
    }

    @Test
    public void daylightSavingChangeDoesNotLoseADay() {
        // Europe/Paris switches to summer time on 29/03/2026: that night lasts 23 hours.
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"));
        assertEquals(2, RentalRules.daysBetween("2026-03-28", "2026-03-30"));
        assertEquals(2, RentalRules.daysBetween("2026-10-24", "2026-10-26"));
    }

    @Test
    public void invalidDatesAreRejected() {
        assertEquals(-1, RentalRules.daysBetween("2026-02-30", "2026-03-02"));
        assertEquals(-1, RentalRules.daysBetween("", "2026-03-02"));
        assertEquals(-1, RentalRules.daysBetween(null, "2026-03-02"));
    }

    @Test
    public void totalPriceIsDaysTimesDailyPrice() {
        assertEquals(1350.0, RentalRules.totalPrice(3, 450), 0.001);
        assertEquals(0.0, RentalRules.totalPrice(0, 450), 0.001);
    }
}
