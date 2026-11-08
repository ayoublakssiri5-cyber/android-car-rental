package com.example.locationvoitures.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Rental rules without Android dependency, so they can be unit-tested on the JVM. */
public final class RentalRules {
    private static final long DAY_MILLIS = 86400000L;

    private RentalRules() { }

    /**
     * Number of rental days between two yyyy-MM-dd dates, or -1 when a date is invalid.
     * Rounded rather than truncated: a daylight-saving change makes one day last 23 hours,
     * which used to remove a day from the price.
     */
    public static long daysBetween(String start, String end) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            format.setLenient(false);
            Date startDate = format.parse(start.trim());
            Date endDate = format.parse(end.trim());
            return Math.round((endDate.getTime() - startDate.getTime()) / (double) DAY_MILLIS);
        } catch (ParseException | NullPointerException exception) {
            return -1;
        }
    }

    public static double totalPrice(long days, double dailyPrice) {
        return days * dailyPrice;
    }
}
