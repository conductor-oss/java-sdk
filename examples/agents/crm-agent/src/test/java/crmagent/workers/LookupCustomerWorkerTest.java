package crmagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LookupCustomerWorkerTest {

    private final LookupCustomerWorker worker = new LookupCustomerWorker();

    @Test
    void taskDefName() {
        assertEquals("cm_lookup_customer", worker.getTaskDefName());
    }

    @Test
    void returnsCustomerProfile() {
        Task task = taskWith(Map.of("customerId", "CUST-4821", "channel", "email"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Acme Corporation", result.getOutputData().get("name"));
        assertEquals("support@acme.com", result.getOutputData().get("email"));
        assertEquals("enterprise", result.getOutputData().get("tier"));
        assertEquals("3 years", result.getOutputData().get("accountAge"));
        assertEquals("$250,000/year", result.getOutputData().get("contractValue"));
        assertEquals("Michael Torres", result.getOutputData().get("assignedRep"));
        assertEquals("manufacturing", result.getOutputData().get("industry"));
    }

    @Test
    void completesWithAnyCustomerId() {
        Task task = taskWith(Map.of("customerId", "CUST-9999", "channel", "phone"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("name"));
        assertNotNull(result.getOutputData().get("tier"));
    }

    @Test
    void handlesNullCustomerId() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", null);
        input.put("channel", "email");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("name"));
    }

    @Test
    void handlesNullChannel() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", "CUST-4821");
        input.put("channel", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("name"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("name"));
        assertNotNull(result.getOutputData().get("email"));
    }

    @Test
    void handlesBlankCustomerId() {
        Task task = taskWith(Map.of("customerId", "  ", "channel", "chat"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("tier"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("customerId", "CUST-4821", "channel", "email"));
        TaskResult result = worker.execute(task);

        Map<String, Object> output = result.getOutputData();
        assertEquals(7, output.size());
        assertTrue(output.containsKey("name"));
        assertTrue(output.containsKey("email"));
        assertTrue(output.containsKey("tier"));
        assertTrue(output.containsKey("accountAge"));
        assertTrue(output.containsKey("contractValue"));
        assertTrue(output.containsKey("assignedRep"));
        assertTrue(output.containsKey("industry"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
