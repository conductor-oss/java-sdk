package documentingestion.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IngestStoreVectorsWorkerTest {

    @TempDir
    Path tempDir;

    @Test
    void taskDefName() {
        IngestStoreVectorsWorker worker = new IngestStoreVectorsWorker(tempDir);
        assertEquals("ingest_store_vectors", worker.getTaskDefName());
    }

    @Test
    void storesVectorsAndReturnsCount() {
        IngestStoreVectorsWorker worker = new IngestStoreVectorsWorker(tempDir);
        List<Map<String, Object>> vectors = createVectors(3);
        Task task = taskWith(new HashMap<>(Map.of("vectors", vectors, "collection", "test_collection")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("storedCount"));
    }

    @Test
    void returnsCollectionName() {
        IngestStoreVectorsWorker worker = new IngestStoreVectorsWorker(tempDir);
        List<Map<String, Object>> vectors = createVectors(1);
        Task task = taskWith(new HashMap<>(Map.of("vectors", vectors, "collection", "knowledge_base")));
        TaskResult result = worker.execute(task);

        assertEquals("knowledge_base", result.getOutputData().get("collection"));
    }

    @Test
    void writesJsonFileToDisk() throws IOException {
        IngestStoreVectorsWorker worker = new IngestStoreVectorsWorker(tempDir);
        List<Map<String, Object>> vectors = createVectors(2);
        Task task = taskWith(new HashMap<>(Map.of("vectors", vectors, "collection", "test_persist")));
        TaskResult result = worker.execute(task);

        String filePath = (String) result.getOutputData().get("filePath");
        assertNotNull(filePath);
        assertFalse(filePath.contains("[error]"));

        File file = new File(filePath);
        assertTrue(file.exists(), "Vector JSON file should exist on disk");

        // Verify the file contains valid JSON with expected structure
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> fileContent = mapper.readValue(file, Map.class);
        assertEquals("test_persist", fileContent.get("collection"));
        assertEquals(2, fileContent.get("vectorCount"));
        assertNotNull(fileContent.get("vectors"));
    }

    @Test
    void handlesEmptyVectorList() {
        IngestStoreVectorsWorker worker = new IngestStoreVectorsWorker(tempDir);
        List<Map<String, Object>> vectors = new ArrayList<>();
        Task task = taskWith(new HashMap<>(Map.of("vectors", vectors, "collection", "empty_test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("storedCount"));
        assertEquals("empty_test", result.getOutputData().get("collection"));
    }

    @Test
    void storedCountMatchesInputSize() {
        IngestStoreVectorsWorker worker = new IngestStoreVectorsWorker(tempDir);
        for (int n : new int[]{1, 5, 10}) {
            List<Map<String, Object>> vectors = createVectors(n);
            Task task = taskWith(new HashMap<>(Map.of("vectors", vectors, "collection", "test")));
            TaskResult result = worker.execute(task);

            assertEquals(n, result.getOutputData().get("storedCount"));
        }
    }

    @Test
    void returnsFilePath() {
        IngestStoreVectorsWorker worker = new IngestStoreVectorsWorker(tempDir);
        List<Map<String, Object>> vectors = createVectors(1);
        Task task = taskWith(new HashMap<>(Map.of("vectors", vectors, "collection", "path_test")));
        TaskResult result = worker.execute(task);

        String filePath = (String) result.getOutputData().get("filePath");
        assertNotNull(filePath);
        assertTrue(filePath.endsWith("_vectors.json"));
    }

    private List<Map<String, Object>> createVectors(int count) {
        List<Map<String, Object>> vectors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> vector = new LinkedHashMap<>();
            vector.put("id", "chunk-" + i);
            vector.put("embedding", List.of(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8));
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("text", "sample text " + i);
            metadata.put("wordCount", 10);
            vector.put("metadata", metadata);
            vectors.add(vector);
        }
        return vectors;
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
