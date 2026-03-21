package eventtransformation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OutputEventWorkerTest {

    private final OutputEventWorker worker = new OutputEventWorker();

    @Test
    void taskDefName() {
        assertEquals("et_output_event", worker.getTaskDefName());
    }

    @Test
    void deliversEventSuccessfully() {
        Task task = taskWith(Map.of(
                "mappedEvent", Map.of(
                        "specversion", "1.0",
                        "type", "com.example.repository.push",
                        "id", "evt-1"),
                "targetFormat", "cloudevents"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("out-fixed-001", result.getOutputData().get("outputEventId"));
        assertEquals(true, result.getOutputData().get("delivered"));
    }

    @Test
    void returnsOutputSizeBytes() {
        Task task = taskWith(Map.of(
                "mappedEvent", Map.of("id", "evt-1"),
                "targetFormat", "cloudevents"));
        TaskResult result = worker.execute(task);

        assertEquals(512, result.getOutputData().get("outputSizeBytes"));
    }

    @Test
    void returnsDeliveredAtTimestamp() {
        Task task = taskWith(Map.of(
                "mappedEvent", Map.of("id", "evt-1"),
                "targetFormat", "cloudevents"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("deliveredAt"));
    }

    @Test
    void returnsFixedOutputEventId() {
        Task task = taskWith(Map.of(
                "mappedEvent", Map.of("id", "evt-1"),
                "targetFormat", "cloudevents"));
        TaskResult result = worker.execute(task);

        assertEquals("out-fixed-001", result.getOutputData().get("outputEventId"));
    }

    @Test
    void handlesEmptyMappedEvent() {
        Task task = taskWith(Map.of(
                "mappedEvent", Map.of(),
                "targetFormat", "cloudevents"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("delivered"));
    }

    @Test
    void handlesMissingMappedEvent() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("out-fixed-001", result.getOutputData().get("outputEventId"));
        assertEquals(true, result.getOutputData().get("delivered"));
    }

    @Test
    void handlesMissingTargetFormat() {
        Map<String, Object> input = new HashMap<>();
        input.put("mappedEvent", Map.of("id", "evt-1"));
        input.put("targetFormat", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(512, result.getOutputData().get("outputSizeBytes"));
    }

    @Test
    void allOutputFieldsPresent() {
        Task task = taskWith(Map.of(
                "mappedEvent", Map.of("id", "evt-1"),
                "targetFormat", "cloudevents"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("outputEventId"));
        assertNotNull(result.getOutputData().get("delivered"));
        assertNotNull(result.getOutputData().get("outputSizeBytes"));
        assertNotNull(result.getOutputData().get("deliveredAt"));
        assertEquals(4, result.getOutputData().size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
