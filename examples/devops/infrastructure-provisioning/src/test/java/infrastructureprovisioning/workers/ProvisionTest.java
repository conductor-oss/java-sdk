package infrastructureprovisioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProvisionTest {

    private final Provision worker = new Provision();

    @Test
    void taskDefName() {
        assertEquals("ip_provision", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith();
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsResourceId() {
        Task task = taskWith();
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("resourceId"));
    }

    @Test
    void resourceIdIsDeterministic() {
        Task task = taskWith();
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);
        assertEquals(r1.getOutputData().get("resourceId"), r2.getOutputData().get("resourceId"));
    }

    @Test
    void outputContainsStatus() {
        Task task = taskWith();
        TaskResult result = worker.execute(task);
        assertEquals("running", result.getOutputData().get("status"));
    }

    @Test
    void outputHasTwoFields() {
        Task task = taskWith();
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("resourceId"));
        assertTrue(result.getOutputData().containsKey("status"));
    }

    @Test
    void deterministicOutput() {
        Task task = taskWith();
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);
        assertEquals(r1.getOutputData(), r2.getOutputData());
    }

    private Task taskWith() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("plan", Map.of("type", "kubernetes-cluster", "region", "us-east-1", "size", "medium"));
        input.put("validated", true);
        task.setInputData(input);
        return task;
    }
}
