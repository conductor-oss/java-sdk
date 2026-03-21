package datacatalog.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiscoverAssetsWorkerTest {

    private final DiscoverAssetsWorker worker = new DiscoverAssetsWorker();

    @Test
    void taskDefName() {
        assertEquals("cg_discover_assets", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("dataSource", Map.of("host", "db.internal")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsFiveAssets() {
        Task task = taskWith(Map.of("dataSource", Map.of("host", "db.internal")));
        TaskResult result = worker.execute(task);
        assertEquals(5, result.getOutputData().get("assetCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void assetsContainExpectedTables() {
        Task task = taskWith(Map.of("dataSource", Map.of()));
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> assets = (List<Map<String, Object>>) result.getOutputData().get("assets");
        assertNotNull(assets);
        assertTrue(assets.stream().anyMatch(a -> "customers".equals(a.get("name"))));
        assertTrue(assets.stream().anyMatch(a -> "orders".equals(a.get("name"))));
        assertTrue(assets.stream().anyMatch(a -> "products".equals(a.get("name"))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void assetsContainSchemaInfo() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> assets = (List<Map<String, Object>>) result.getOutputData().get("assets");
        assertTrue(assets.stream().anyMatch(a -> "analytics".equals(a.get("schema"))));
        assertTrue(assets.stream().anyMatch(a -> "reporting".equals(a.get("schema"))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void assetsHaveColumns() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> assets = (List<Map<String, Object>>) result.getOutputData().get("assets");
        for (Map<String, Object> asset : assets) {
            List<String> cols = (List<String>) asset.get("columns");
            assertNotNull(cols);
            assertFalse(cols.isEmpty());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void dailyRevenueIsView() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> assets = (List<Map<String, Object>>) result.getOutputData().get("assets");
        Map<String, Object> view = assets.stream()
                .filter(a -> "daily_revenue".equals(a.get("name"))).findFirst().orElse(null);
        assertNotNull(view);
        assertEquals("view", view.get("type"));
    }

    @Test
    void assetCountMatchesListSize() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> assets = (List<Map<String, Object>>) result.getOutputData().get("assets");
        assertEquals(assets.size(), result.getOutputData().get("assetCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
