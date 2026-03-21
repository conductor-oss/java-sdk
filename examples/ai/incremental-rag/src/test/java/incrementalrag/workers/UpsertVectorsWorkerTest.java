package incrementalrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpsertVectorsWorkerTest {

    private final UpsertVectorsWorker worker = new UpsertVectorsWorker();

    @Test
    void taskDefName() {
        assertEquals("ir_upsert_vectors", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void countsInsertsAndUpdates() {
        List<Map<String, Object>> embeddings = List.of(
                Map.of("id", "doc-101", "vector", List.of(0.12), "action", "update"),
                Map.of("id", "doc-205", "vector", List.of(0.12), "action", "insert"),
                Map.of("id", "doc-307", "vector", List.of(0.12), "action", "update"),
                Map.of("id", "doc-412", "vector", List.of(0.12), "action", "insert")
        );
        List<String> docIds = List.of("doc-101", "doc-205", "doc-307", "doc-412");

        Task task = taskWith(Map.of("embeddings", embeddings, "docIds", docIds));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4, result.getOutputData().get("upsertedCount"));
        assertEquals(2, result.getOutputData().get("inserted"));
        assertEquals(2, result.getOutputData().get("updated"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsDocIds() {
        List<Map<String, Object>> embeddings = List.of(
                Map.of("id", "doc-101", "vector", List.of(0.12), "action", "insert")
        );
        List<String> docIds = List.of("doc-101");

        Task task = taskWith(Map.of("embeddings", embeddings, "docIds", docIds));
        TaskResult result = worker.execute(task);

        List<String> outputDocIds = (List<String>) result.getOutputData().get("docIds");
        assertNotNull(outputDocIds);
        assertEquals(1, outputDocIds.size());
        assertEquals("doc-101", outputDocIds.get(0));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
