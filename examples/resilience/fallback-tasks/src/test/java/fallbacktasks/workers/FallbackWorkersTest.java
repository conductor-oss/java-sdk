package fallbacktasks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FallbackWorkersTest {

    // ---- PrimaryApiWorker tests ----

    @Test
    void primaryApiTaskDefName() {
        PrimaryApiWorker worker = new PrimaryApiWorker();
        assertEquals("fb_primary_api", worker.getTaskDefName());
    }

    @Test
    void primaryApiAvailableReturnsOkWithData() {
        PrimaryApiWorker worker = new PrimaryApiWorker();
        Task task = taskWith(Map.of("available", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ok", result.getOutputData().get("apiStatus"));
        assertEquals("primary", result.getOutputData().get("source"));
        assertNotNull(result.getOutputData().get("data"));
    }

    @Test
    void primaryApiUnavailableReturnsUnavailable() {
        PrimaryApiWorker worker = new PrimaryApiWorker();
        Task task = taskWith(Map.of("available", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unavailable", result.getOutputData().get("apiStatus"));
        assertNull(result.getOutputData().get("data"));
        assertNull(result.getOutputData().get("source"));
    }

    @Test
    void primaryApiDefaultsToAvailableWhenNoInput() {
        PrimaryApiWorker worker = new PrimaryApiWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ok", result.getOutputData().get("apiStatus"));
        assertEquals("primary", result.getOutputData().get("source"));
    }

    @Test
    void primaryApiHandlesStringAvailableTrue() {
        PrimaryApiWorker worker = new PrimaryApiWorker();
        Task task = taskWith(Map.of("available", "true"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ok", result.getOutputData().get("apiStatus"));
    }

    @Test
    void primaryApiHandlesStringAvailableFalse() {
        PrimaryApiWorker worker = new PrimaryApiWorker();
        Task task = taskWith(Map.of("available", "false"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unavailable", result.getOutputData().get("apiStatus"));
    }

    // ---- SecondaryApiWorker tests ----

    @Test
    void secondaryApiTaskDefName() {
        SecondaryApiWorker worker = new SecondaryApiWorker();
        assertEquals("fb_secondary_api", worker.getTaskDefName());
    }

    @Test
    void secondaryApiReturnsSecondaryData() {
        SecondaryApiWorker worker = new SecondaryApiWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("secondary", result.getOutputData().get("source"));
        assertNotNull(result.getOutputData().get("data"));
    }

    @Test
    void secondaryApiAlwaysCompletes() {
        SecondaryApiWorker worker = new SecondaryApiWorker();
        Task task = taskWith(Map.of("unexpected", "input"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("secondary", result.getOutputData().get("source"));
    }

    // ---- CacheLookupWorker tests ----

    @Test
    void cacheLookupTaskDefName() {
        CacheLookupWorker worker = new CacheLookupWorker();
        assertEquals("fb_cache_lookup", worker.getTaskDefName());
    }

    @Test
    void cacheLookupReturnsCacheData() {
        CacheLookupWorker worker = new CacheLookupWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("cache", result.getOutputData().get("source"));
        assertNotNull(result.getOutputData().get("data"));
        assertEquals(true, result.getOutputData().get("stale"));
    }

    @Test
    void cacheLookupAlwaysCompletes() {
        CacheLookupWorker worker = new CacheLookupWorker();
        Task task = taskWith(Map.of("irrelevant", 42));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("cache", result.getOutputData().get("source"));
        assertEquals(true, result.getOutputData().get("stale"));
    }

    // ---- helper ----

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
