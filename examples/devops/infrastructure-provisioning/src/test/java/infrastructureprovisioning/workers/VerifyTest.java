package infrastructureprovisioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VerifyTest {

    private final Verify worker = new Verify();

    @Test
    void taskDefName() {
        assertEquals("ip_verify", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith("RES-500001");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputVerifiedIsTrue() {
        Task task = taskWith("RES-500001");
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("verified"));
    }

    @Test
    void outputContainsVerified() {
        Task task = taskWith("RES-500001");
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("verified"));
    }

    @Test
    void handlesNullResourceId() {
        Task task = taskWith(null);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void deterministicOutput() {
        Task task = taskWith("RES-500001");
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);
        assertEquals(r1.getOutputData(), r2.getOutputData());
    }

    @Test
    void differentResourceIdsSameResult() {
        Task t1 = taskWith("RES-500001");
        Task t2 = taskWith("RES-999999");
        TaskResult r1 = worker.execute(t1);
        TaskResult r2 = worker.execute(t2);
        assertEquals(r1.getOutputData().get("verified"), r2.getOutputData().get("verified"));
    }

    private Task taskWith(String resourceId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("resourceId", resourceId);
        task.setInputData(input);
        return task;
    }
}
