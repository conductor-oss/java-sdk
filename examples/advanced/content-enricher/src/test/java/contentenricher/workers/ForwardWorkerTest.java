package contentenricher.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ForwardWorkerTest {

    private final ForwardWorker worker = new ForwardWorker();

    @Test
    void taskDefName() {
        assertEquals("enr_forward", worker.getTaskDefName());
    }

    @Test
    void forwardsSuccessfully() {
        Task task = taskWith(Map.of("enrichedMessage", Map.of("customerId", "C1", "tier", "gold")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("forwarded"));
    }

    @Test
    void destinationIsRoutedByCustomerId() {
        Task task = taskWith(Map.of("enrichedMessage", Map.of("customerId", "C2")));
        TaskResult result = worker.execute(task);

        String destination = (String) result.getOutputData().get("destination");
        assertNotNull(destination);
        assertTrue(destination.startsWith("order_processing_queue_p"),
                "Destination should be a partitioned queue, got: " + destination);
    }

    @Test
    void sameCustomerIdRoutesDeterministically() {
        Task task1 = taskWith(Map.of("enrichedMessage", Map.of("customerId", "C99")));
        Task task2 = taskWith(Map.of("enrichedMessage", Map.of("customerId", "C99")));

        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("destination"), r2.getOutputData().get("destination"));
    }

    @Test
    void outputContainsContentHash() {
        Task task = taskWith(Map.of("enrichedMessage", Map.of("customerId", "C1")));
        TaskResult result = worker.execute(task);

        String hash = (String) result.getOutputData().get("contentHash");
        assertNotNull(hash);
        assertEquals(64, hash.length(), "SHA-256 hash should be 64 hex chars");
    }

    @Test
    void handlesNullEnrichedMessage() {
        Map<String, Object> input = new HashMap<>();
        input.put("enrichedMessage", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("forwarded"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyEnrichedMessage() {
        Task task = taskWith(Map.of("enrichedMessage", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("forwarded"));
    }

    @Test
    void outputContainsAllFields() {
        Task task = taskWith(Map.of("enrichedMessage", Map.of("x", 1)));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("forwarded"));
        assertNotNull(result.getOutputData().get("destination"));
        assertNotNull(result.getOutputData().get("contentHash"));
    }

    @Test
    void handlesMessageWithManyFields() {
        Task task = taskWith(Map.of("enrichedMessage",
                Map.of("customerId", "C99", "tier", "platinum", "region", "EU", "creditLimit", 100000)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("forwarded"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
