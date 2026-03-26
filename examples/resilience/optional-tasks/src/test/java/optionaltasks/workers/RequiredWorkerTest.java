package optionaltasks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequiredWorkerTest {

    @Test
    void taskDefName() {
        RequiredWorker worker = new RequiredWorker();
        assertEquals("opt_required", worker.getTaskDefName());
    }

    @Test
    void processesDataSuccessfully() {
        RequiredWorker worker = new RequiredWorker();
        Task task = taskWith(Map.of("data", "hello"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed-hello", result.getOutputData().get("result"));
    }

    @Test
    void processesEmptyData() {
        RequiredWorker worker = new RequiredWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed-", result.getOutputData().get("result"));
    }

    @Test
    void processesNullData() {
        RequiredWorker worker = new RequiredWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("data", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed-", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsResultKey() {
        RequiredWorker worker = new RequiredWorker();
        Task task = taskWith(Map.of("data", "test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
    }

    @Test
    void resultIsDeterministic() {
        RequiredWorker worker = new RequiredWorker();
        Task task1 = taskWith(Map.of("data", "abc"));
        Task task2 = taskWith(Map.of("data", "abc"));
        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("result"), result2.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
