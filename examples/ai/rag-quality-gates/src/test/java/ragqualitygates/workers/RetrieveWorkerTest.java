package ragqualitygates.workers;

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
        assertEquals("qg_retrieve", worker.getTaskDefName());
    }

    @Test
    void returnsThreeDocuments() {
        Task task = taskWith(new HashMap<>(Map.of("question", "How does Conductor work?")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertNotNull(documents);
        assertEquals(3, documents.size());
    }

    @Test
    void documentsHaveIdTextAndScore() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");

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
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");

        assertEquals(0.92, documents.get(0).get("score"));
        assertEquals(0.78, documents.get(1).get("score"));
        assertEquals(0.55, documents.get(2).get("score"));
    }

    @Test
    void documentsContainExpectedContent() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");

        assertTrue(((String) documents.get(0).get("text")).contains("orchestrates"));
        assertTrue(((String) documents.get(1).get("text")).contains("poll"));
        assertTrue(((String) documents.get(2).get("text")).contains("versioning"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
