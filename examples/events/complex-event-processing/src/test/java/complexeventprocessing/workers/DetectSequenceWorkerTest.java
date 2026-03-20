package complexeventprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectSequenceWorkerTest {

    private final DetectSequenceWorker worker = new DetectSequenceWorker();

    @Test
    void taskDefName() {
        assertEquals("cp_detect_sequence", worker.getTaskDefName());
    }

    @Test
    void detectsLoginBeforePurchase() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "login", "ts", 1000),
                Map.of("type", "browse", "ts", 1500),
                Map.of("type", "purchase", "ts", 2000));
        Task task = taskWith(Map.of("events", events, "rule", "login_then_purchase"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("detected"));
        assertEquals("login_then_purchase", result.getOutputData().get("rule"));
    }

    @Test
    void notDetectedWhenPurchaseBeforeLogin() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "purchase", "ts", 1000),
                Map.of("type", "login", "ts", 2000));
        Task task = taskWith(Map.of("events", events, "rule", "login_then_purchase"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("detected"));
    }

    @Test
    void notDetectedWhenNoLogin() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "browse", "ts", 1000),
                Map.of("type", "purchase", "ts", 2000));
        Task task = taskWith(Map.of("events", events, "rule", "login_then_purchase"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("detected"));
    }

    @Test
    void notDetectedWhenNoPurchase() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "login", "ts", 1000),
                Map.of("type", "browse", "ts", 2000));
        Task task = taskWith(Map.of("events", events, "rule", "login_then_purchase"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("detected"));
    }

    @Test
    void handlesEmptyEventsList() {
        Task task = taskWith(Map.of("events", List.of(), "rule", "login_then_purchase"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("detected"));
    }

    @Test
    void handlesNullEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", null);
        input.put("rule", "login_then_purchase");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("detected"));
    }

    @Test
    void handlesNullRule() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", List.of(Map.of("type", "login", "ts", 1000)));
        input.put("rule", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("rule"));
    }

    @Test
    void detectsLoginImmediatelyBeforePurchase() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "login", "ts", 1000),
                Map.of("type", "purchase", "ts", 1001));
        Task task = taskWith(Map.of("events", events, "rule", "login_then_purchase"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("detected"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
