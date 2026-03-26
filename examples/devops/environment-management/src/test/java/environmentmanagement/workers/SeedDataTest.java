package environmentmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SeedDataTest {

    private final SeedData worker = new SeedData();

    @Test
    void taskDefName() {
        assertEquals("em_seed_data", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith("ENV-300001");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsSeeded() {
        Task task = taskWith("ENV-300001");
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("seeded"));
    }

    @Test
    void outputContainsRecords() {
        Task task = taskWith("ENV-300001");
        TaskResult result = worker.execute(task);
        assertEquals(1000, result.getOutputData().get("records"));
    }

    @Test
    void outputHasTwoFields() {
        Task task = taskWith("ENV-300001");
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("seeded"));
        assertTrue(result.getOutputData().containsKey("records"));
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

    private Task taskWith(String envId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("envId", envId);
        task.setInputData(input);
        return task;
    }
}
