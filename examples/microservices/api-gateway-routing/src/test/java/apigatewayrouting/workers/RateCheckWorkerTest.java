package apigatewayrouting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RateCheckWorkerTest {

    private final RateCheckWorker worker = new RateCheckWorker();

    @Test
    void taskDefName() {
        assertEquals("gw_rate_check", worker.getTaskDefName());
    }

    @Test
    void allowsRequestWithinLimit() {
        Task task = taskWith(Map.of("clientId", "client-42", "path", "/api/orders"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("allowed"));
        assertEquals(55, result.getOutputData().get("remaining"));
    }

    @Test
    void handlesNullClientId() {
        Map<String, Object> input = new HashMap<>();
        input.put("clientId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("allowed"));
    }

    @Test
    void outputContainsBothFields() {
        Task task = taskWith(Map.of("clientId", "c1", "path", "/p"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("allowed"));
        assertTrue(result.getOutputData().containsKey("remaining"));
    }

    @Test
    void remainingIsPositive() {
        Task task = taskWith(Map.of("clientId", "client-42"));
        TaskResult result = worker.execute(task);

        int remaining = (int) result.getOutputData().get("remaining");
        assertTrue(remaining > 0);
    }

    @Test
    void isDeterministic() {
        Task t1 = taskWith(Map.of("clientId", "c1"));
        Task t2 = taskWith(Map.of("clientId", "c2"));
        assertEquals(worker.execute(t1).getOutputData().get("remaining"),
                     worker.execute(t2).getOutputData().get("remaining"));
    }

    @Test
    void differentClientsGetSameResult() {
        Task t1 = taskWith(Map.of("clientId", "client-a"));
        Task t2 = taskWith(Map.of("clientId", "client-b"));
        TaskResult r1 = worker.execute(t1);
        TaskResult r2 = worker.execute(t2);

        assertEquals(r1.getOutputData().get("allowed"), r2.getOutputData().get("allowed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
