package complexeventprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectAbsenceWorkerTest {

    private final DetectAbsenceWorker worker = new DetectAbsenceWorker();

    @Test
    void taskDefName() {
        assertEquals("cp_detect_absence", worker.getTaskDefName());
    }

    @Test
    void detectsAbsenceOfConfirmation() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "login", "ts", 1000),
                Map.of("type", "purchase", "ts", 2000));
        Task task = taskWith(Map.of("events", events, "rule", "expected_confirmation"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("detected"));
        assertEquals("expected_confirmation", result.getOutputData().get("rule"));
    }

    @Test
    void notDetectedWhenConfirmationPresent() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "login", "ts", 1000),
                Map.of("type", "confirmation", "ts", 2000),
                Map.of("type", "purchase", "ts", 3000));
        Task task = taskWith(Map.of("events", events, "rule", "expected_confirmation"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("detected"));
    }

    @Test
    void detectsAbsenceInEmptyList() {
        Task task = taskWith(Map.of("events", List.of(), "rule", "expected_confirmation"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("detected"));
    }

    @Test
    void handlesNullEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", null);
        input.put("rule", "expected_confirmation");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("detected"));
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
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("detected"));
    }

    @Test
    void confirmationAtEndOfList() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "login", "ts", 1000),
                Map.of("type", "purchase", "ts", 2000),
                Map.of("type", "confirmation", "ts", 3000));
        Task task = taskWith(Map.of("events", events, "rule", "expected_confirmation"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("detected"));
    }

    @Test
    void confirmationOnlyEvent() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "confirmation", "ts", 1000));
        Task task = taskWith(Map.of("events", events, "rule", "expected_confirmation"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("detected"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
