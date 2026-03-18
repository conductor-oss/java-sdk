package sagapattern.workers;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared in-memory booking store for saga pattern workers.
 * Tracks hotel reservations, flight bookings, and payment transactions.
 */
public final class BookingStore {

    static final ConcurrentHashMap<String, String> HOTEL_RESERVATIONS = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, String> FLIGHT_BOOKINGS = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, String> PAYMENT_TRANSACTIONS = new ConcurrentHashMap<>();

    private BookingStore() {}

    /** Clear all bookings. For testing only. */
    public static void clear() {
        HOTEL_RESERVATIONS.clear();
        FLIGHT_BOOKINGS.clear();
        PAYMENT_TRANSACTIONS.clear();
    }
}
