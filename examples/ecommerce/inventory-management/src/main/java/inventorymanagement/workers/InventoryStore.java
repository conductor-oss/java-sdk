package inventorymanagement.workers;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe in-memory inventory store using ConcurrentHashMap and CAS operations.
 *
 * Provides:
 *   - Stock level tracking per SKU per warehouse
 *   - Atomic reservation with CAS (compare-and-swap) for thread safety
 *   - Reservation tracking with expiration
 *   - Backorder management
 *   - Audit trail for all inventory changes
 */
public final class InventoryStore {

    /** Composite key: "warehouseId:sku" -> available quantity. */
    private static final ConcurrentHashMap<String, AtomicInteger> STOCK = new ConcurrentHashMap<>();

    /** Reserved quantities per reservation ID. */
    private static final ConcurrentHashMap<String, ReservationRecord> RESERVATIONS = new ConcurrentHashMap<>();

    /** Backorder queue: sku -> list of backorder requests. */
    private static final ConcurrentHashMap<String, List<Map<String, Object>>> BACKORDERS = new ConcurrentHashMap<>();

    /** Audit log: list of all inventory changes. */
    private static final List<Map<String, Object>> AUDIT_LOG = Collections.synchronizedList(new ArrayList<>());

    private InventoryStore() {}

    /** Record for a reservation. */
    public record ReservationRecord(String sku, String warehouseId, int qty, Instant createdAt, Instant expiresAt) {}

    // --- Stock Management ---

    /** Set the stock level for a SKU at a warehouse. */
    public static void setStock(String warehouseId, String sku, int qty) {
        String key = key(warehouseId, sku);
        STOCK.put(key, new AtomicInteger(qty));
        audit("SET_STOCK", warehouseId, sku, qty, "Initial stock set");
    }

    /** Get the current available stock for a SKU at a warehouse. */
    public static int getStock(String warehouseId, String sku) {
        AtomicInteger stock = STOCK.get(key(warehouseId, sku));
        return stock != null ? stock.get() : 0;
    }

    /** Get the current available stock across all warehouses. */
    public static int getTotalStock(String sku) {
        int total = 0;
        for (Map.Entry<String, AtomicInteger> entry : STOCK.entrySet()) {
            if (entry.getKey().endsWith(":" + sku)) {
                total += entry.getValue().get();
            }
        }
        return total;
    }

    // --- Reservation with CAS ---

    /**
     * Atomically reserve stock using compare-and-swap.
     * Returns the reservation ID if successful, null if insufficient stock.
     */
    public static String reserve(String warehouseId, String sku, int qty, String reservationId) {
        String key = key(warehouseId, sku);
        AtomicInteger stock = STOCK.get(key);
        if (stock == null) return null;

        // CAS loop for thread-safe reservation
        while (true) {
            int current = stock.get();
            if (current < qty) {
                return null; // Insufficient stock
            }
            if (stock.compareAndSet(current, current - qty)) {
                // Reservation successful
                Instant now = Instant.now();
                RESERVATIONS.put(reservationId, new ReservationRecord(
                        sku, warehouseId, qty, now, now.plusSeconds(900) // 15 min expiry
                ));
                audit("RESERVE", warehouseId, sku, -qty,
                        "Reserved " + qty + " units, reservation " + reservationId);
                return reservationId;
            }
            // CAS failed, retry
        }
    }

    /**
     * Release a reservation, returning the stock to available.
     */
    public static boolean release(String reservationId) {
        ReservationRecord rec = RESERVATIONS.remove(reservationId);
        if (rec == null) return false;

        String key = key(rec.warehouseId(), rec.sku());
        AtomicInteger stock = STOCK.get(key);
        if (stock != null) {
            stock.addAndGet(rec.qty());
            audit("RELEASE", rec.warehouseId(), rec.sku(), rec.qty(),
                    "Released reservation " + reservationId);
        }
        return true;
    }

    /**
     * Fulfill a reservation: permanently deduct the stock (no return).
     */
    public static boolean fulfill(String reservationId) {
        ReservationRecord rec = RESERVATIONS.remove(reservationId);
        if (rec == null) return false;

        audit("FULFILL", rec.warehouseId(), rec.sku(), 0,
                "Fulfilled reservation " + reservationId + " for " + rec.qty() + " units");
        return true;
    }

    // --- Deduction (direct, no reservation) ---

    /**
     * Directly deduct stock (for fulfillment without prior reservation).
     * Uses CAS for thread safety. Returns true if deduction succeeded.
     */
    public static boolean deduct(String warehouseId, String sku, int qty) {
        String key = key(warehouseId, sku);
        AtomicInteger stock = STOCK.get(key);
        if (stock == null) return false;

        while (true) {
            int current = stock.get();
            if (current < qty) return false;
            if (stock.compareAndSet(current, current - qty)) {
                audit("DEDUCT", warehouseId, sku, -qty, "Deducted " + qty + " units on fulfillment");
                return true;
            }
        }
    }

    /** Add stock (e.g., receiving a shipment). */
    public static void addStock(String warehouseId, String sku, int qty) {
        String key = key(warehouseId, sku);
        STOCK.computeIfAbsent(key, k -> new AtomicInteger(0)).addAndGet(qty);
        audit("ADD_STOCK", warehouseId, sku, qty, "Added " + qty + " units");
    }

    // --- Backorders ---

    /** Create a backorder for a SKU. */
    public static void addBackorder(String sku, int qty, String orderId) {
        List<Map<String, Object>> orders = BACKORDERS.computeIfAbsent(sku,
                k -> Collections.synchronizedList(new ArrayList<>()));
        Map<String, Object> backorder = new LinkedHashMap<>();
        backorder.put("orderId", orderId);
        backorder.put("qty", qty);
        backorder.put("createdAt", Instant.now().toString());
        backorder.put("status", "PENDING");
        orders.add(backorder);
    }

    /** Get pending backorders for a SKU. */
    public static List<Map<String, Object>> getBackorders(String sku) {
        return BACKORDERS.getOrDefault(sku, List.of());
    }

    // --- Audit ---

    private static void audit(String action, String warehouseId, String sku, int delta, String note) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("action", action);
        entry.put("warehouseId", warehouseId);
        entry.put("sku", sku);
        entry.put("delta", delta);
        entry.put("note", note);
        entry.put("timestamp", Instant.now().toString());
        AUDIT_LOG.add(entry);
    }

    /** Get the full audit log. */
    public static List<Map<String, Object>> getAuditLog() {
        return Collections.unmodifiableList(new ArrayList<>(AUDIT_LOG));
    }

    /** Get reservation details. */
    public static ReservationRecord getReservation(String reservationId) {
        return RESERVATIONS.get(reservationId);
    }

    // --- Utility ---

    private static String key(String warehouseId, String sku) {
        return warehouseId + ":" + sku;
    }

    /** Reset all inventory state. Used in tests. */
    public static void reset() {
        STOCK.clear();
        RESERVATIONS.clear();
        BACKORDERS.clear();
        AUDIT_LOG.clear();
    }

    /** Seed with default inventory. */
    public static void seedDefaults() {
        setStock("WH-EAST-01", "WH-1000XM5", 45);
        setStock("WH-EAST-01", "LAPTOP-PRO", 30);
        setStock("WH-EAST-01", "USB-C-HUB", 150);
        setStock("WH-WEST-01", "WH-1000XM5", 60);
        setStock("WH-WEST-01", "LAPTOP-PRO", 25);
    }
}
