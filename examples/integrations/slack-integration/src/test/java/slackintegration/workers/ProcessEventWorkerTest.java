package slackintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessEventWorkerTest {

    private final ProcessEventWorker worker = new ProcessEventWorker();

    @Test
    void taskDefName() {
        assertEquals("slk_process_event", worker.getTaskDefName());
    }

    @Test
    void processesDeploymentEvent() {
        Task task = taskWith(Map.of("eventType", "deployment", "data", Map.of("service", "api-gateway")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(((String) result.getOutputData().get("message")).contains("deployment"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void processesAlertEvent() {
        Task task = taskWith(Map.of("eventType", "alert", "data", Map.of("severity", "high")));
        TaskResult result = worker.execute(task);

        assertTrue(((String) result.getOutputData().get("message")).contains("alert"));
    }

    @Test
    void handlesNullEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", null);
        input.put("data", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(((String) result.getOutputData().get("message")).contains("unknown"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", "test");
        input.put("data", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(((String) result.getOutputData().get("message")).contains("test"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void messageIncludesEventTypeAndData() {
        Task task = taskWith(Map.of("eventType", "build", "data", "some-data"));
        TaskResult result = worker.execute(task);

        String message = (String) result.getOutputData().get("message");
        assertTrue(message.contains("build"));
        assertTrue(message.contains("some-data"));
    }

    @Test
    void alwaysMarksProcessedTrue() {
        Task task = taskWith(Map.of("eventType", "any", "data", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
