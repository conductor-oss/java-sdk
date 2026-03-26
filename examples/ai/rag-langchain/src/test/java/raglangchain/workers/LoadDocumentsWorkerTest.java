package raglangchain.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadDocumentsWorkerTest {

    private final LoadDocumentsWorker worker = new LoadDocumentsWorker();

    @Test
    void taskDefName() {
        assertEquals("lc_load_documents", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsFourDocuments() {
        Task task = taskWith(Map.of("sourceUrl", "https://example.com/docs"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, Object>> documents =
                (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertNotNull(documents);
        assertEquals(4, documents.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void documentsHavePageContentAndMetadata() {
        Task task = taskWith(Map.of("sourceUrl", "https://example.com/docs"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> documents =
                (List<Map<String, Object>>) result.getOutputData().get("documents");
        Map<String, Object> firstDoc = documents.get(0);
        assertNotNull(firstDoc.get("pageContent"));
        assertTrue(firstDoc.get("pageContent") instanceof String);

        Map<String, Object> metadata = (Map<String, Object>) firstDoc.get("metadata");
        assertNotNull(metadata);
        assertEquals("https://example.com/docs", metadata.get("source"));
        assertEquals(1, metadata.get("page"));
    }

    @Test
    void returnsDocCountAndLoader() {
        Task task = taskWith(Map.of("sourceUrl", "https://example.com/docs"));
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("docCount"));
        assertEquals("WebBaseLoader", result.getOutputData().get("loader"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesDefaultUrlWhenNotProvided() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, Object>> documents =
                (List<Map<String, Object>>) result.getOutputData().get("documents");
        Map<String, Object> metadata = (Map<String, Object>) documents.get(0).get("metadata");
        assertEquals("https://example.com/docs", metadata.get("source"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
