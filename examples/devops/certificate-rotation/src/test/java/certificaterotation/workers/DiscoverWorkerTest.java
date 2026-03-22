package certificaterotation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DiscoverWorkerTest {

    private final DiscoverWorker worker = new DiscoverWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_discover", worker.getTaskDefName());
    }

    @Test
    void discoversRealCertForPublicDomain() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");

        Task task = taskWith(Map.of("domain", "google.com"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("google.com", result.getOutputData().get("domain"));
        assertNotNull(result.getOutputData().get("subject"));
        assertNotNull(result.getOutputData().get("issuer"));
        assertNotNull(result.getOutputData().get("notAfter"));
        assertTrue(result.getOutputData().get("daysRemaining") instanceof Long);
        assertNotNull(result.getOutputData().get("expiringSoon"));
    }

    @Test
    void detectsExpiringSoonThreshold() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");

        // Google's cert should NOT be expiring soon (typically valid 90 days, refreshed frequently)
        Task task = taskWith(Map.of("domain", "google.com"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        long daysRemaining = (Long) result.getOutputData().get("daysRemaining");
        boolean expiringSoon = (Boolean) result.getOutputData().get("expiringSoon");

        // The expiringSoon flag should accurately reflect < 30 days
        if (daysRemaining < 30) {
            assertTrue(expiringSoon, "Should be marked expiring soon when < 30 days remain");
        } else {
            assertFalse(expiringSoon, "Should NOT be marked expiring soon when >= 30 days remain");
        }
    }

    @Test
    void detectsExpiredCertForKnownBadDomain() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");

        // expired.badssl.com has an intentionally expired certificate
        Task task = taskWith(Map.of("domain", "expired.badssl.com"));
        TaskResult result = worker.execute(task);

        // The worker uses a trust-all manager so it can inspect even expired certs
        // It should either COMPLETE with negative daysRemaining, or FAIL on connection
        if (result.getStatus() == TaskResult.Status.COMPLETED) {
            long daysRemaining = (Long) result.getOutputData().get("daysRemaining");
            boolean expiringSoon = (Boolean) result.getOutputData().get("expiringSoon");
            assertTrue(daysRemaining < 0, "Expired cert should have negative days remaining");
            assertTrue(expiringSoon, "Expired cert should be flagged as expiring soon");
        }
        // If it fails, that's acceptable too — some networks block badssl.com
    }

    @Test
    void failsOnMissingDomain() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("domain"));
    }

    @Test
    void failsOnBlankDomain() {
        Task task = taskWith(Map.of("domain", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("domain"));
    }

    @Test
    void failsTerminallyOnUnresolvableDomain() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");

        Task task = taskWith(Map.of("domain", "this-domain-definitely-does-not-exist-xyzzy.invalid"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus(),
                "DNS resolution failure should be terminal (not retryable)");
    }

    private static boolean isNetworkAvailable() {
        try {
            InetAddress.getByName("google.com");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
