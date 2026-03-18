package idempotentprocessing.workers;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory deduplication store shared across workers within a single JVM session.
 *
 * Maps messageId → resultHash for messages that have been successfully processed.
 * In production, replace this with Redis, DynamoDB, or a relational database so the
 * store survives restarts and is shared across instances.
 */
public final class DedupStore {

    private static final ConcurrentHashMap<String, String> PROCESSED = new ConcurrentHashMap<>();

    private DedupStore() {}

    /** Returns true if the messageId has already been recorded as processed. */
    public static boolean isProcessed(String messageId) {
        return PROCESSED.containsKey(messageId);
    }

    /** Returns the resultHash stored for a previously processed messageId, or null. */
    public static String getResultHash(String messageId) {
        return PROCESSED.get(messageId);
    }

    /** Records a messageId as processed with the given resultHash. */
    public static void record(String messageId, String resultHash) {
        PROCESSED.put(messageId, resultHash);
    }

    /** Clears all entries. Intended for testing only. */
    public static void clear() {
        PROCESSED.clear();
    }

    /** Returns the number of entries in the store. */
    public static int size() {
        return PROCESSED.size();
    }
}
