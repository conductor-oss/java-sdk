package ordermanagement.workers;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared in-memory order store used by all order management workers.
 * Thread-safe via ConcurrentHashMap.
 *
 * Provides order state machine validation:
 *   CREATED -> CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED
 *   Any state -> CANCELLED
 */
public final class OrderStore {

    /** Valid state transitions. Maps fromState -> set of allowed toStates. */
    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
            "CREATED", Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", Set.of("PROCESSING", "CANCELLED"),
            "PROCESSING", Set.of("SHIPPED", "CANCELLED"),
            "SHIPPED", Set.of("DELIVERED", "CANCELLED"),
            "DELIVERED", Set.of(),  // terminal state
            "CANCELLED", Set.of()   // terminal state
    );

    private static final ConcurrentHashMap<String, Map<String, Object>> ORDERS = new ConcurrentHashMap<>();

    private OrderStore() {}

    /** Store or update an order. */
    public static void put(String orderId, Map<String, Object> order) {
        ORDERS.put(orderId, order);
    }

    /** Retrieve an order by ID. */
    public static Map<String, Object> get(String orderId) {
        return ORDERS.get(orderId);
    }

    /**
     * Attempt a state transition. Returns true if the transition was valid and applied.
     * Records the transition in the order history.
     */
    @SuppressWarnings("unchecked")
    public static boolean transition(String orderId, String newState, String actor, String note) {
        Map<String, Object> order = ORDERS.get(orderId);
        if (order == null) return false;

        synchronized (order) {
            String currentState = (String) order.get("status");
            Set<String> allowed = VALID_TRANSITIONS.getOrDefault(currentState, Set.of());

            if (!allowed.contains(newState)) {
                return false;
            }

            order.put("status", newState);
            order.put("lastUpdated", Instant.now().toString());

            // Add to history
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("from", currentState);
            entry.put("to", newState);
            entry.put("timestamp", Instant.now().toString());
            entry.put("actor", actor);
            entry.put("note", note);

            List<Map<String, Object>> history = (List<Map<String, Object>>) order.get("history");
            if (history != null) {
                history.add(entry);
            }

            return true;
        }
    }

    /**
     * Check if a transition from current state to newState is valid.
     */
    public static boolean isValidTransition(String orderId, String newState) {
        Map<String, Object> order = ORDERS.get(orderId);
        if (order == null) return false;
        String currentState = (String) order.get("status");
        return VALID_TRANSITIONS.getOrDefault(currentState, Set.of()).contains(newState);
    }

    /** Get the current state of an order. */
    public static String getStatus(String orderId) {
        Map<String, Object> order = ORDERS.get(orderId);
        return order != null ? (String) order.get("status") : null;
    }

    /** Get the order history. */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getHistory(String orderId) {
        Map<String, Object> order = ORDERS.get(orderId);
        if (order == null) return List.of();
        return (List<Map<String, Object>>) order.getOrDefault("history", List.of());
    }

    /** Reset all state. Used in tests. */
    public static void reset() {
        ORDERS.clear();
    }
}
