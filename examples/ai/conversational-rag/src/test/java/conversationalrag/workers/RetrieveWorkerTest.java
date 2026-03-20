package conversationalrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RetrieveWorkerTest {

    private final RetrieveWorker worker = new RetrieveWorker();

    @Test
    void taskDefName() {
        assertEquals("crag_retrieve", worker.getTaskDefName());
    }

    @Test
    void returnsThreeDocuments() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, -0.3, 0.5),
                "contextualQuery", "test query"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertNotNull(documents);
        assertEquals(3, documents.size());
    }

    @Test
    void documentsHaveTextAndScore() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");

        for (Map<String, Object> doc : documents) {
            assertNotNull(doc.get("text"));
            assertNotNull(doc.get("score"));
            assertInstanceOf(String.class, doc.get("text"));
            assertInstanceOf(Double.class, doc.get("score"));
        }
    }

    @Test
    void documentsHaveExpectedScores() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");

        assertEquals(0.93, documents.get(0).get("score"));
        assertEquals(0.88, documents.get(1).get("score"));
        assertEquals(0.84, documents.get(2).get("score"));
    }

    @Test
    void documentsContainExpectedContent() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");

        assertTrue(((String) documents.get(0).get("text")).contains("versioning"));
        assertTrue(((String) documents.get(1).get("text")).contains("workers"));
        assertTrue(((String) documents.get(2).get("text")).contains("JSON"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
