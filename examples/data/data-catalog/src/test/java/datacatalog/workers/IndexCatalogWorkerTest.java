package datacatalog.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndexCatalogWorkerTest {

    private final IndexCatalogWorker worker = new IndexCatalogWorker();

    @Test
    void taskDefName() {
        assertEquals("cg_index_catalog", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("taggedAssets", List.of(Map.of("name", "t1"))));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void indexedCountMatchesInput() {
        Task task = taskWith(Map.of("taggedAssets", List.of(
                Map.of("name", "t1"), Map.of("name", "t2"), Map.of("name", "t3"))));
        TaskResult result = worker.execute(task);
        assertEquals(3, result.getOutputData().get("indexedCount"));
    }

    @Test
    void returnsCatalogId() {
        Task task = taskWith(Map.of("taggedAssets", List.of(Map.of("name", "t1"))));
        TaskResult result = worker.execute(task);
        assertEquals("CAT-2024-0315", result.getOutputData().get("catalogId"));
    }

    @Test
    void returnsSearchableTrue() {
        Task task = taskWith(Map.of("taggedAssets", List.of(Map.of("name", "t1"))));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("searchable"));
    }

    @Test
    void handlesEmptyAssets() {
        Task task = taskWith(Map.of("taggedAssets", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals(0, result.getOutputData().get("indexedCount"));
    }

    @Test
    void handlesNullAssets() {
        Map<String, Object> input = new HashMap<>();
        input.put("taggedAssets", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("indexedCount"));
    }

    @Test
    void fiveAssetsIndexed() {
        Task task = taskWith(Map.of("taggedAssets", List.of(
                Map.of("name", "a"), Map.of("name", "b"), Map.of("name", "c"),
                Map.of("name", "d"), Map.of("name", "e"))));
        TaskResult result = worker.execute(task);
        assertEquals(5, result.getOutputData().get("indexedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
