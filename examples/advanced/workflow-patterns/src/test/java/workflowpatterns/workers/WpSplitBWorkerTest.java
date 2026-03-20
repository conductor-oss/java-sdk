package workflowpatterns.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WpSplitBWorkerTest {

    private final WpSplitBWorker worker = new WpSplitBWorker();

    @Test
    void taskDefName() {
        assertEquals("wp_split_b", worker.getTaskDefName());
    }

    @Test
    void branchBCompletesSuccessfully() {
        Task task = taskWith(Map.of("branch", "B", "chainOutput", "chain_processed"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("branch_b_done", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsValue58() {
        Task task = taskWith(Map.of("branch", "B", "chainOutput", "chain_processed"));
        TaskResult result = worker.execute(task);

        assertEquals(58, result.getOutputData().get("value"));
    }

    @Test
    void handlesNullChainOutput() {
        Map<String, Object> input = new HashMap<>();
        input.put("branch", "B");
        input.put("chainOutput", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingChainOutput() {
        Task task = taskWith(Map.of("branch", "B"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("branch_b_done", result.getOutputData().get("result"));
    }

    @Test
    void valueIsDeterministic() {
        Task task = taskWith(Map.of("chainOutput", "test"));
        TaskResult result = worker.execute(task);

        assertEquals(58, result.getOutputData().get("value"));
        assertEquals("branch_b_done", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
