package humantask.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessFormWorkerTest {

    @Test
    void taskDefName() {
        ProcessFormWorker worker = new ProcessFormWorker();
        assertEquals("ht_process_form", worker.getTaskDefName());
    }

    @Test
    void returnsApprovedWhenApprovedIsTrue() {
        ProcessFormWorker worker = new ProcessFormWorker();
        Task task = taskWith(Map.of("approved", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("application-approved", result.getOutputData().get("decision"));
    }

    @Test
    void returnsRejectedWhenApprovedIsFalse() {
        ProcessFormWorker worker = new ProcessFormWorker();
        Task task = taskWith(Map.of("approved", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("application-rejected", result.getOutputData().get("decision"));
    }

    @Test
    void returnsRejectedWhenApprovedIsMissing() {
        ProcessFormWorker worker = new ProcessFormWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("application-rejected", result.getOutputData().get("decision"));
    }

    @Test
    void returnsRejectedWhenApprovedIsNull() {
        ProcessFormWorker worker = new ProcessFormWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("approved", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("application-rejected", result.getOutputData().get("decision"));
    }

    @Test
    void returnsRejectedWhenApprovedIsNonBoolean() {
        ProcessFormWorker worker = new ProcessFormWorker();
        Task task = taskWith(Map.of("approved", "yes"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("application-rejected", result.getOutputData().get("decision"));
    }

    @Test
    void outputContainsDecisionKey() {
        ProcessFormWorker worker = new ProcessFormWorker();
        Task task = taskWith(Map.of("approved", true));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("decision"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
