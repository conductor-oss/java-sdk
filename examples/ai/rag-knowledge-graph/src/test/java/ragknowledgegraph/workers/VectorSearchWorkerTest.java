package ragknowledgegraph.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VectorSearchWorkerTest {

    private final VectorSearchWorker worker = new VectorSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("kg_vector_search", worker.getTaskDefName());
    }

    @Test
    void returnsFourDocuments() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Conductor?",
                "entityHints", List.of(Map.of("name", "Conductor"))
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents =
                (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertNotNull(documents);
        assertEquals(4, documents.size());
    }

    @Test
    void documentsHaveRequiredFields() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents =
                (List<Map<String, Object>>) result.getOutputData().get("documents");

        for (Map<String, Object> doc : documents) {
            assertNotNull(doc.get("id"));
            assertNotNull(doc.get("text"));
            assertNotNull(doc.get("score"));
            assertInstanceOf(String.class, doc.get("id"));
            assertInstanceOf(String.class, doc.get("text"));
            assertInstanceOf(Double.class, doc.get("score"));
        }
    }

    @Test
    void documentsHaveExpectedScores() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents =
                (List<Map<String, Object>>) result.getOutputData().get("documents");

        assertEquals(0.94, documents.get(0).get("score"));
        assertEquals(0.91, documents.get(1).get("score"));
        assertEquals(0.87, documents.get(2).get("score"));
        assertEquals(0.85, documents.get(3).get("score"));
    }

    @Test
    void documentsContainExpectedContent() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents =
                (List<Map<String, Object>>) result.getOutputData().get("documents");

        assertTrue(((String) documents.get(0).get("text")).contains("Netflix"));
        assertTrue(((String) documents.get(1).get("text")).contains("JSON"));
        assertTrue(((String) documents.get(2).get("text")).contains("orchestration"));
        assertTrue(((String) documents.get(3).get("text")).contains("Orkes"));
    }

    @Test
    void documentsHaveUniqueIds() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents =
                (List<Map<String, Object>>) result.getOutputData().get("documents");

        long uniqueIds = documents.stream()
                .map(d -> d.get("id"))
                .distinct()
                .count();
        assertEquals(4, uniqueIds);
    }

    @Test
    void handlesMissingQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("documents"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
