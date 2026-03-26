package chaininghttptasks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrepareRequestWorkerTest {

    private final PrepareRequestWorker worker = new PrepareRequestWorker();

    @Test
    void taskDefName() {
        assertEquals("http_prepare_request", worker.getTaskDefName());
    }

    @Test
    void preparesSearchTermFromQuery() {
        Task task = taskWith(Map.of("query", "http_chain_demo"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("http_chain_demo", result.getOutputData().get("searchTerm"));
    }

    @Test
    void defaultsToDefaultWhenQueryMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default", result.getOutputData().get("searchTerm"));
    }

    @Test
    void defaultsToDefaultWhenQueryBlank() {
        Task task = taskWith(Map.of("query", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default", result.getOutputData().get("searchTerm"));
    }

    @Test
    void outputContainsSearchTermField() {
        Task task = taskWith(Map.of("query", "my_workflow"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("searchTerm"));
        assertEquals("my_workflow", result.getOutputData().get("searchTerm"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
