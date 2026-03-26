package environmentmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VerifyEnvTest {

    private final VerifyEnv worker = new VerifyEnv();

    @Test
    void taskDefName() {
        assertEquals("em_verify", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith("ENV-300001");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputHealthyIsTrue() {
        Task task = taskWith("ENV-300001");
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("healthy"));
    }

    @Test
    void outputContainsHealthy() {
        Task task = taskWith("ENV-300001");
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("healthy"));
    }

    @Test
    void deterministicOutput() {
        Task task = taskWith("ENV-300001");
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);
        assertEquals(r1.getOutputData(), r2.getOutputData());
    }

    @Test
    void handlesNullEnvId() {
        Task task = taskWith(null);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void differentEnvIdsSameResult() {
        Task t1 = taskWith("ENV-300001");
        Task t2 = taskWith("ENV-999999");
        TaskResult r1 = worker.execute(t1);
        TaskResult r2 = worker.execute(t2);
        assertEquals(r1.getOutputData().get("healthy"), r2.getOutputData().get("healthy"));
    }

    private Task taskWith(String envId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("envId", envId);
        task.setInputData(input);
        return task;
    }
}
