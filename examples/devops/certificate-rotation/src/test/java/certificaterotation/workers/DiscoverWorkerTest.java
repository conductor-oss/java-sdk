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
