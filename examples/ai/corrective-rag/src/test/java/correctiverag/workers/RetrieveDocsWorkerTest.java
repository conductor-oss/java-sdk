package correctiverag.workers;

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
    void returnsThreeDocuments() {
        Task task = taskWith(new HashMap<>(Map.of("question", "What is Conductor pricing?")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs = (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertNotNull(docs);
        assertEquals(3, docs.size());
    }

    @Test
    void offTopicQueryProducesLowRelevance() {
        // "pricing" is not in the technical knowledge base, so scores should be low
        Task task = taskWith(new HashMap<>(Map.of("question", "pricing")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs = (List<Map<String, Object>>) result.getOutputData().get("documents");
        for (Map<String, Object> doc : docs) {
            double relevance = ((Number) doc.get("relevance")).doubleValue();
            assertTrue(relevance < 0.5, "Expected low relevance for off-topic query but got " + relevance);
        }
    }

    @Test
    void onTopicQueryProducesHigherRelevance() {
        // "Conductor dynamic fork parallel task" should match well
        Task task = taskWith(new HashMap<>(Map.of("question", "Conductor dynamic fork parallel task")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs = (List<Map<String, Object>>) result.getOutputData().get("documents");
        double topRelevance = ((Number) docs.get(0).get("relevance")).doubleValue();
        assertTrue(topRelevance > 0.1, "On-topic query should have some relevance, got " + topRelevance);
    }

    @Test
    void documentsContainRequiredFields() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs = (List<Map<String, Object>>) result.getOutputData().get("documents");
        for (Map<String, Object> doc : docs) {
            assertNotNull(doc.get("id"));
            assertNotNull(doc.get("text"));
            assertNotNull(doc.get("relevance"));
        }
    }

    @Test
    void documentsAreSortedByRelevanceDescending() {
        Task task = taskWith(new HashMap<>(Map.of("question", "Conductor workflow")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs = (List<Map<String, Object>>) result.getOutputData().get("documents");
        for (int i = 0; i < docs.size() - 1; i++) {
            double a = ((Number) docs.get(i).get("relevance")).doubleValue();
            double b = ((Number) docs.get(i + 1).get("relevance")).doubleValue();
            assertTrue(a >= b, "Documents should be sorted by relevance descending");
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
