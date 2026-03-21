package dashboarddata.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CacheDashboardWorkerTest {

    private final CacheDashboardWorker worker = new CacheDashboardWorker();

    @Test
    void taskDefName() {
        assertEquals("dh_cache_dashboard", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("dashboardId", "dash-001", "widgets", List.of(), "refreshInterval", 300));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsCacheKey() {
        Task task = taskWith(Map.of("dashboardId", "dash-001", "widgets", List.of(), "refreshInterval", 300));
        TaskResult result = worker.execute(task);
        String cacheKey = (String) result.getOutputData().get("cacheKey");
        assertTrue(cacheKey.startsWith("dashboard:dash-001:"));
    }

    @Test
    void returnsTtl() {
        Task task = taskWith(Map.of("dashboardId", "dash-001", "widgets", List.of(), "refreshInterval", 600));
        TaskResult result = worker.execute(task);
        assertEquals("600s", result.getOutputData().get("ttl"));
    }

    @Test
    void returnsReady() {
        Task task = taskWith(Map.of("dashboardId", "dash-001", "widgets", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void usesDefaultDashboardId() {
        Task task = taskWith(Map.of("widgets", List.of()));
        TaskResult result = worker.execute(task);
        String cacheKey = (String) result.getOutputData().get("cacheKey");
        assertTrue(cacheKey.startsWith("dashboard:unknown:"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
