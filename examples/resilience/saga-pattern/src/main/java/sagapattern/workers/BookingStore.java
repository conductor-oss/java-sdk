package sagapattern.workers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared in-memory booking store for saga pattern workers.
 * Tracks hotel reservations, flight bookings, and payment transactions.
 * Also tracks action ordering for compensation verification.
 */
public final class BookingStore {

    static final ConcurrentHashMap<String, String> HOTEL_RESERVATIONS = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, String> FLIGHT_BOOKINGS = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, String> PAYMENT_TRANSACTIONS = new ConcurrentHashMap<>();

    /** Ordered list of actions (e.g., "BOOK_FLIGHT:FLT-TRIP-1", "CANCEL_FLIGHT:FLT-TRIP-1") */
    private static final List<String> ACTION_LOG = Collections.synchronizedList(new ArrayList<>());

    private BookingStore() {}

    /** Record an action for ordering verification. */
    public static void recordAction(String action, String resourceId) {
        ACTION_LOG.add(action + ":" + resourceId);
    }

    /** Get a snapshot of all recorded actions in order. */
    public static List<String> getActionLog() {
        return new ArrayList<>(ACTION_LOG);
    }

    /** Check if a flight booking exists. */
    public static boolean hasFlightBooking(String bookingId) {
        return FLIGHT_BOOKINGS.containsKey(bookingId);
    }

    /** Check if a hotel reservation exists. */
    public static boolean hasHotelReservation(String reservationId) {
        return HOTEL_RESERVATIONS.containsKey(reservationId);
    }

    /** Check if a payment transaction exists. */
    public static boolean hasPaymentTransaction(String transactionId) {
        return PAYMENT_TRANSACTIONS.containsKey(transactionId);
    }

    /** Check if all stores are empty. */
    public static boolean isEmpty() {
        return FLIGHT_BOOKINGS.isEmpty() && HOTEL_RESERVATIONS.isEmpty() && PAYMENT_TRANSACTIONS.isEmpty();
    }

    /** Clear all bookings and action log. For testing only. */
    public static void clear() {
        HOTEL_RESERVATIONS.clear();
        FLIGHT_BOOKINGS.clear();
        PAYMENT_TRANSACTIONS.clear();
        ACTION_LOG.clear();
    }
}
