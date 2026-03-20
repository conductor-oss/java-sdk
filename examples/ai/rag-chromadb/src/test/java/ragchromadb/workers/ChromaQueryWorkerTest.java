package ragchromadb.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChromaQueryWorkerTest {

    private final ChromaQueryWorker worker = new ChromaQueryWorker();

    @Test
    void taskDefName() {
        assertEquals("chroma_query", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsChromaDbResults() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, 0.2, 0.3),
                "collection", "product_docs",
                "nResults", 3
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> results = (Map<String, Object>) result.getOutputData().get("results");
        assertNotNull(results);

        // Verify ids
        List<List<String>> ids = (List<List<String>>) results.get("ids");
        assertEquals(1, ids.size());
        assertEquals(List.of("id-101", "id-204", "id-089"), ids.get(0));

        // Verify documents
        List<List<String>> documents = (List<List<String>>) results.get("documents");
        assertEquals(1, documents.size());
        assertEquals(3, documents.get(0).size());
        assertTrue(documents.get(0).get(0).contains("ChromaDB"));

        // Verify metadatas
        List<List<Map<String, Object>>> metadatas = (List<List<Map<String, Object>>>) results.get("metadatas");
        assertEquals(1, metadatas.size());
        assertEquals("readme.md", metadatas.get(0).get(0).get("source"));

        // Verify distances
        List<List<Double>> distances = (List<List<Double>>) results.get("distances");
        assertEquals(1, distances.size());
        assertEquals(0.12, distances.get(0).get(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsCollectionInfo() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, 0.2),
                "collection", "my_collection",
                "nResults", 3
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> collectionInfo = (Map<String, Object>) result.getOutputData().get("collectionInfo");
        assertNotNull(collectionInfo);
        assertEquals("my_collection", collectionInfo.get("name"));
        assertEquals("localhost:8000", collectionInfo.get("host"));
        assertEquals(3492, collectionInfo.get("count"));
        assertEquals("default", collectionInfo.get("embeddingFunction"));
    }

    @Test
    void defaultsNResultsTo3() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, 0.2),
                "collection", "test_coll"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void collectionNameFromInput() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.5),
                "collection", "custom_collection",
                "nResults", 5
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> collectionInfo = (Map<String, Object>) result.getOutputData().get("collectionInfo");
        assertEquals("custom_collection", collectionInfo.get("name"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
