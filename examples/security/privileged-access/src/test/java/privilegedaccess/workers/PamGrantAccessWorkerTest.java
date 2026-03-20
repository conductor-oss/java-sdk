package privilegedaccess.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PamGrantAccessWorkerTest {

    private final PamGrantAccessWorker worker = new PamGrantAccessWorker();

    @Test
    void taskDefName() {
        assertEquals("pam_grant_access", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("resource", "prod-db", "duration", "2h"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsGrantAccessFlag() {
        Task task = taskWith(Map.of("resource", "prod-db", "duration", "2h"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("grant_access"));
    }

    @Test
    void outputContainsProcessedFlag() {
        Task task = taskWith(Map.of("resource", "prod-db", "duration", "2h"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesMissingResource() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingDuration() {
        Task task = taskWith(Map.of("resource", "staging-db"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("resource", null);
        input.put("duration", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputIsDeterministic() {
        Task task = taskWith(Map.of("resource", "api-gateway", "duration", "4h"));
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);

        assertEquals(r1.getOutputData().get("grant_access"), r2.getOutputData().get("grant_access"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
