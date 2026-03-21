package eventfiltering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClassifyPriorityWorkerTest {

    private final ClassifyPriorityWorker worker = new ClassifyPriorityWorker();

    @Test
    void taskDefName() {
        assertEquals("ef_classify_priority", worker.getTaskDefName());
    }

    @Test
    void classifiesCriticalAsUrgent() {
        Task task = taskWith(Map.of(
                "eventType", "system.alert",
                "severity", "critical",
                "metadata", Map.of("source", "monitoring-agent")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("urgent", result.getOutputData().get("priority"));
        assertEquals("routed_to_urgent", result.getOutputData().get("filterResult"));
    }

    @Test
    void classifiesHighAsUrgent() {
        Task task = taskWith(Map.of(
                "eventType", "system.alert",
                "severity", "high",
                "metadata", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("urgent", result.getOutputData().get("priority"));
        assertEquals("routed_to_urgent", result.getOutputData().get("filterResult"));
    }

    @Test
    void classifiesMediumAsStandard() {
        Task task = taskWith(Map.of(
                "eventType", "metric.update",
                "severity", "medium",
                "metadata", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("standard", result.getOutputData().get("priority"));
        assertEquals("routed_to_standard", result.getOutputData().get("filterResult"));
    }

    @Test
    void classifiesLowAsStandard() {
        Task task = taskWith(Map.of(
                "eventType", "info",
                "severity", "low",
                "metadata", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("standard", result.getOutputData().get("priority"));
        assertEquals("routed_to_standard", result.getOutputData().get("filterResult"));
    }

    @Test
    void classifiesUnknownSeverityAsDrop() {
        Task task = taskWith(Map.of(
                "eventType", "system.alert",
                "severity", "unknown_level",
                "metadata", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("drop", result.getOutputData().get("priority"));
        assertEquals("dropped", result.getOutputData().get("filterResult"));
        assertEquals("Unknown severity level: unknown_level", result.getOutputData().get("dropReason"));
    }

    @Test
    void setsDropReasonWithSeverityValue() {
        Task task = taskWith(Map.of(
                "eventType", "system.alert",
                "severity", "extreme",
                "metadata", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("drop", result.getOutputData().get("priority"));
        assertEquals("Unknown severity level: extreme", result.getOutputData().get("dropReason"));
    }

    @Test
    void dropReasonIsNullForUrgent() {
        Task task = taskWith(Map.of(
                "eventType", "system.alert",
                "severity", "critical",
                "metadata", Map.of()));
        TaskResult result = worker.execute(task);

        assertNull(result.getOutputData().get("dropReason"));
    }

    @Test
    void handlesNullSeverity() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", "system.alert");
        input.put("severity", null);
        input.put("metadata", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("drop", result.getOutputData().get("priority"));
        assertEquals("Unknown severity level: ", result.getOutputData().get("dropReason"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("drop", result.getOutputData().get("priority"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
