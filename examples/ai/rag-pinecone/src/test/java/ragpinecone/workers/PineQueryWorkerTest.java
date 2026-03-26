package ragpinecone.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PineQueryWorkerTest {

    private final PineQueryWorker worker = new PineQueryWorker();

    @Test
    void taskDefName() {
        assertEquals("pine_query", worker.getTaskDefName());
    }

    @Test
    void returnsThreeMatches() {
        Task task = taskWith(Map.of(
                "embedding", List.of(0.1, 0.2, 0.3),
                "namespace", "default",
                "topK", 3
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches =
                (List<Map<String, Object>>) result.getOutputData().get("matches");
        assertNotNull(matches);
        assertEquals(3, matches.size());
    }

    @Test
    void firstMatchHasCorrectFields() {
        Task task = taskWith(Map.of(
                "embedding", List.of(0.1, 0.2),
                "namespace", "test-ns",
                "topK", 3
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches =
                (List<Map<String, Object>>) result.getOutputData().get("matches");

        Map<String, Object> first = matches.get(0);
        assertEquals("vec-001", first.get("id"));
        assertEquals(0.96, first.get("score"));

        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) first.get("metadata");
        assertEquals("Pinecone is a managed vector database for ML applications.", meta.get("text"));
        assertEquals("technical", meta.get("category"));
    }

    @Test
    void secondAndThirdMatchIds() {
        Task task = taskWith(Map.of(
                "embedding", List.of(0.5),
                "namespace", "default",
                "topK", 3
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches =
                (List<Map<String, Object>>) result.getOutputData().get("matches");

        assertEquals("vec-002", matches.get(1).get("id"));
        assertEquals(0.91, matches.get(1).get("score"));
        assertEquals("vec-003", matches.get(2).get("id"));
        assertEquals(0.87, matches.get(2).get("score"));
    }

    @Test
    void returnsStats() {
        Task task = taskWith(Map.of(
                "embedding", List.of(0.1),
                "namespace", "default",
                "topK", 3
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) result.getOutputData().get("stats");
        assertNotNull(stats);
        assertEquals(0.23, stats.get("indexFullness"));
        assertEquals(1536, stats.get("dimension"));

        @SuppressWarnings("unchecked")
        Map<String, Object> namespaces = (Map<String, Object>) stats.get("namespaces");
        assertNotNull(namespaces);
        assertTrue(namespaces.containsKey("default"));
        assertTrue(namespaces.containsKey("staging"));
    }

    @Test
    void defaultsNamespaceWhenMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "topK", 3
        )));
        task.getInputData().put("namespace", null);
        TaskResult result = worker.execute(task);

        assertEquals("default", result.getOutputData().get("namespace"));
    }

    @Test
    void returnsNamespaceInOutput() {
        Task task = taskWith(Map.of(
                "embedding", List.of(0.1),
                "namespace", "production",
                "topK", 5
        ));
        TaskResult result = worker.execute(task);

        assertEquals("production", result.getOutputData().get("namespace"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
