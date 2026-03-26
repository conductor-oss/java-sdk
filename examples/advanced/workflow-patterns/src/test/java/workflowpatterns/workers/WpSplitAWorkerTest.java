package workflowpatterns.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WpSplitAWorkerTest {

    private final WpSplitAWorker worker = new WpSplitAWorker();

    @Test
    void taskDefName() {
        assertEquals("wp_split_a", worker.getTaskDefName());
    }

    @Test
    void branchACompletesSuccessfully() {
        Task task = taskWith(Map.of("branch", "A", "chainOutput", "chain_processed"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("branch_a_done", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsValue42() {
        Task task = taskWith(Map.of("branch", "A", "chainOutput", "chain_processed"));
        TaskResult result = worker.execute(task);

        assertEquals(42, result.getOutputData().get("value"));
    }

    @Test
    void handlesNullChainOutput() {
        Map<String, Object> input = new HashMap<>();
        input.put("branch", "A");
        input.put("chainOutput", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingChainOutput() {
        Task task = taskWith(Map.of("branch", "A"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("branch_a_done", result.getOutputData().get("result"));
    }

    @Test
    void resultIsDeterministic() {
        Task task = taskWith(Map.of("chainOutput", "test"));
        TaskResult result1 = worker.execute(task);
        TaskResult result2 = worker.execute(task);

        assertEquals(result1.getOutputData().get("result"), result2.getOutputData().get("result"));
        assertEquals(result1.getOutputData().get("value"), result2.getOutputData().get("value"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
