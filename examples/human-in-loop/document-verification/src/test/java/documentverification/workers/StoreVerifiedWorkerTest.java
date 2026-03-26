package documentverification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StoreVerifiedWorkerTest {

    @Test
    void taskDefName() {
        StoreVerifiedWorker worker = new StoreVerifiedWorker();
        assertEquals("dv_store_verified", worker.getTaskDefName());
    }

    @Test
    void returnsStoredTrue() {
        StoreVerifiedWorker worker = new StoreVerifiedWorker();
        Task task = taskWith(Map.of("verifiedData", Map.of("name", "Jane Doe")));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("stored"));
    }

    @Test
    void outputContainsStoredKey() {
        StoreVerifiedWorker worker = new StoreVerifiedWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("stored"));
    }

    @Test
    void alwaysCompletes() {
        StoreVerifiedWorker worker = new StoreVerifiedWorker();
        Task task = taskWith(Map.of("extra", "data"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("stored"));
    }

    @Test
    void worksWithEmptyInput() {
        StoreVerifiedWorker worker = new StoreVerifiedWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("stored"));
    }

    @Test
    void isDeterministic() {
        StoreVerifiedWorker worker = new StoreVerifiedWorker();
        Task task1 = taskWith(Map.of("verifiedData", Map.of("name", "Jane")));
        Task task2 = taskWith(Map.of("verifiedData", Map.of("name", "John")));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("stored"), result2.getOutputData().get("stored"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
