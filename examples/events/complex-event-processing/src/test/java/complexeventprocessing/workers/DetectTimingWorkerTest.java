package complexeventprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectTimingWorkerTest {

    private final DetectTimingWorker worker = new DetectTimingWorker();

    @Test
    void taskDefName() {
        assertEquals("cp_detect_timing", worker.getTaskDefName());
    }

    @Test
    void detectsTimingViolation() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "login", "ts", 1000),
                Map.of("type", "browse", "ts", 1500),
                Map.of("type", "purchase", "ts", 8000));
        Task task = taskWith(Map.of("events", events, "maxGapMs", 5000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("violation"));
        assertEquals("pattern_found", result.getOutputData().get("overallResult"));
    }

    @Test
    void noViolationWhenWithinLimits() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "login", "ts", 1000),
                Map.of("type", "browse", "ts", 2000),
                Map.of("type", "purchase", "ts", 3000));
        Task task = taskWith(Map.of("events", events, "maxGapMs", 5000));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("violation"));
        assertEquals("normal", result.getOutputData().get("overallResult"));
    }

    @Test
    void noViolationWithSingleEvent() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "login", "ts", 1000));
        Task task = taskWith(Map.of("events", events, "maxGapMs", 5000));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("violation"));
        assertEquals("normal", result.getOutputData().get("overallResult"));
    }

    @Test
    void noViolationWithEmptyEvents() {
        Task task = taskWith(Map.of("events", List.of(), "maxGapMs", 5000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("violation"));
        assertEquals("normal", result.getOutputData().get("overallResult"));
    }

    @Test
    void handlesNullEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", null);
        input.put("maxGapMs", 5000);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("violation"));
        assertEquals("normal", result.getOutputData().get("overallResult"));
    }

    @Test
    void usesDefaultMaxGapWhenMissing() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "login", "ts", 1000),
                Map.of("type", "purchase", "ts", 7000));
        Task task = taskWith(Map.of("events", events));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("violation"));
        assertEquals("pattern_found", result.getOutputData().get("overallResult"));
    }

    @Test
    void exactlyAtLimitIsNotViolation() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "login", "ts", 1000),
                Map.of("type", "purchase", "ts", 6000));
        Task task = taskWith(Map.of("events", events, "maxGapMs", 5000));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("violation"));
        assertEquals("normal", result.getOutputData().get("overallResult"));
    }

    @Test
    void violationOnFirstGap() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "login", "ts", 1000),
                Map.of("type", "browse", "ts", 20000),
                Map.of("type", "purchase", "ts", 20500));
        Task task = taskWith(Map.of("events", events, "maxGapMs", 5000));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("violation"));
        assertEquals("pattern_found", result.getOutputData().get("overallResult"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
