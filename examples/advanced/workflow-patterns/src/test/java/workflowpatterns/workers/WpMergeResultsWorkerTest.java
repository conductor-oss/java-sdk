package workflowpatterns.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WpMergeResultsWorkerTest {

    private final WpMergeResultsWorker worker = new WpMergeResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("wp_merge_results", worker.getTaskDefName());
    }

    @Test
    void mergesResultsSuccessfully() {
        Task task = taskWith(Map.of("resultA", "branch_a_done", "resultB", "branch_b_done"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("merged_a_b", result.getOutputData().get("combined"));
    }

    @Test
    void outputContainsTotal100() {
        Task task = taskWith(Map.of("resultA", "branch_a_done", "resultB", "branch_b_done"));
        TaskResult result = worker.execute(task);

        assertEquals(100, result.getOutputData().get("total"));
    }

    @Test
    void handlesNullResultA() {
        Map<String, Object> input = new HashMap<>();
        input.put("resultA", null);
        input.put("resultB", "branch_b_done");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("merged_a_b", result.getOutputData().get("combined"));
    }

    @Test
    void handlesNullResultB() {
        Map<String, Object> input = new HashMap<>();
        input.put("resultA", "branch_a_done");
        input.put("resultB", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("merged_a_b", result.getOutputData().get("combined"));
    }

    @Test
    void combinedIsDeterministic() {
        Task task = taskWith(Map.of("resultA", "x", "resultB", "y"));
        TaskResult result = worker.execute(task);

        assertEquals("merged_a_b", result.getOutputData().get("combined"));
        assertEquals(100, result.getOutputData().get("total"));
    }

    @Test
    void outputHasBothKeys() {
        Task task = taskWith(Map.of("resultA", "a", "resultB", "b"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("combined"));
        assertTrue(result.getOutputData().containsKey("total"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
