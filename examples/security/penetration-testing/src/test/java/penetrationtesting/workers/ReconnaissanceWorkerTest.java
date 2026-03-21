package penetrationtesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReconnaissanceWorkerTest {

    private final ReconnaissanceWorker worker = new ReconnaissanceWorker();

    @Test
    void taskDefName() {
        assertEquals("pen_reconnaissance", worker.getTaskDefName());
    }

    @Test
    void reconnaissanceWithTarget() {
        Task task = taskWith(Map.of("target", "api.example.com", "scope", "external"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("RECONNAISSANCE-1382", result.getOutputData().get("reconnaissanceId"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void reconnaissanceWithInternalScope() {
        Task task = taskWith(Map.of("target", "internal.corp.net", "scope", "internal"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("RECONNAISSANCE-1382", result.getOutputData().get("reconnaissanceId"));
    }

    @Test
    void handlesNullTarget() {
        Map<String, Object> input = new HashMap<>();
        input.put("target", null);
        input.put("scope", "external");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("RECONNAISSANCE-1382", result.getOutputData().get("reconnaissanceId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of("target", "web.example.com", "scope", "full"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("reconnaissanceId"));
        assertTrue(result.getOutputData().containsKey("success"));
    }

    @Test
    void reconnaissanceWithIpTarget() {
        Task task = taskWith(Map.of("target", "192.168.1.100", "scope", "network"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void successIsAlwaysTrue() {
        Task task = taskWith(Map.of("target", "test.example.com"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("success"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
