package emailapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrepareWorkerTest {

    @Test
    void taskDefName() {
        PrepareWorker worker = new PrepareWorker();
        assertEquals("ea_prepare", worker.getTaskDefName());
    }

    @Test
    void returnsReadyTrue() {
        PrepareWorker worker = new PrepareWorker();
        Task task = taskWith(new HashMap<>(Map.of("requester", "user@example.com")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void completesWithEmptyInput() {
        PrepareWorker worker = new PrepareWorker();
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void outputContainsReadyKey() {
        PrepareWorker worker = new PrepareWorker();
        Task task = taskWith(new HashMap<>(Map.of("subject", "Expense Report")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("ready"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
