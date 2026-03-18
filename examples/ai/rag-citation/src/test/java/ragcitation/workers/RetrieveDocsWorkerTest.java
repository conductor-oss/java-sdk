package ragcitation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RetrieveDocsWorkerTest {

    private final RetrieveDocsWorker worker = new RetrieveDocsWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_retrieve_docs", worker.getTaskDefName());
    }

    @Test
    void returnsFourDocuments() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test query")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertNotNull(documents);
        assertEquals(4, documents.size());
    }

    @Test
    void documentsHaveRequiredFields() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");

        for (Map<String, Object> doc : documents) {
            assertNotNull(doc.get("id"));
            assertNotNull(doc.get("title"));
            assertNotNull(doc.get("page"));
            assertNotNull(doc.get("text"));
            assertNotNull(doc.get("relevance"));
            assertInstanceOf(String.class, doc.get("id"));
            assertInstanceOf(String.class, doc.get("title"));
            assertInstanceOf(Integer.class, doc.get("page"));
            assertInstanceOf(String.class, doc.get("text"));
            assertInstanceOf(Double.class, doc.get("relevance"));
        }
    }

    @Test
    void documentsHaveExpectedRelevanceScores() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");

        assertEquals(0.95, documents.get(0).get("relevance"));
        assertEquals(0.91, documents.get(1).get("relevance"));
        assertEquals(0.87, documents.get(2).get("relevance"));
        assertEquals(0.83, documents.get(3).get("relevance"));
    }

    @Test
    void documentsHaveExpectedIds() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");

        assertEquals("doc-1", documents.get(0).get("id"));
        assertEquals("doc-2", documents.get(1).get("id"));
        assertEquals("doc-3", documents.get(2).get("id"));
        assertEquals("doc-4", documents.get(3).get("id"));
    }

    @Test
    void documentsContainExpectedContent() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");

        assertTrue(((String) documents.get(0).get("text")).contains("task-based workflow model"));
        assertTrue(((String) documents.get(1).get("text")).contains("any language"));
        assertTrue(((String) documents.get(2).get("text")).contains("multiple versions"));
        assertTrue(((String) documents.get(3).get("text")).contains("load balancing"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
