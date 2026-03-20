package teamsintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatCardWorkerTest {

    private final FormatCardWorker worker = new FormatCardWorker();

    @Test
    void taskDefName() {
        assertEquals("tms_format_card", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void formatsAdaptiveCard() {
        Task task = taskWith(Map.of("eventType", "alert", "data", Map.of("severity", "high")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> card = (Map<String, Object>) result.getOutputData().get("card");
        assertNotNull(card);
        assertEquals("AdaptiveCard", card.get("type"));
        assertEquals("1.4", card.get("version"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void cardBodyContainsAlertText() {
        Task task = taskWith(Map.of("eventType", "alert", "data", Map.of("msg", "test")));
        TaskResult result = worker.execute(task);

        Map<String, Object> card = (Map<String, Object>) result.getOutputData().get("card");
        assertNotNull(card.get("body"));
    }

    @Test
    void handlesNullEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", null);
        input.put("data", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", "alert");
        input.put("data", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("card"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void cardVersionIs14() {
        Task task = taskWith(Map.of("eventType", "test", "data", Map.of()));
        TaskResult result = worker.execute(task);

        Map<String, Object> card = (Map<String, Object>) result.getOutputData().get("card");
        assertEquals("1.4", card.get("version"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
