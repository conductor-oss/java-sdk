package basicrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchVectorsWorkerTest {

    private final SearchVectorsWorker worker = new SearchVectorsWorker();

    @Test
    void taskDefName() {
        assertEquals("brag_search_vectors", worker.getTaskDefName());
    }

    @Test
    void returnsRequestedNumberOfDocuments() {
        Task task = taskWith(new HashMap<>(Map.of(
                "queryText", "What is Conductor orchestration?",
                "topK", 3
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertNotNull(documents);
        assertFalse(documents.isEmpty(), "Search should return at least one document");
        assertTrue(documents.size() <= 3, "Should not exceed requested topK");
    }

    @Test
    void eachDocumentContainsTextAndScore() {
        Task task = taskWith(new HashMap<>(Map.of(
                "queryText", "orchestration workflows microservices",
                "topK", 3
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertNotNull(documents);

        for (Map<String, Object> doc : documents) {
            assertNotNull(doc.get("id"), "Each document must have an id");

            String text = (String) doc.get("text");
            assertNotNull(text, "Each document must have text");
            assertFalse(text.isBlank(), "Document text must not be blank");

            Object scoreObj = doc.get("score");
            assertNotNull(scoreObj, "Each document must have a score");
            double score = ((Number) scoreObj).doubleValue();
            assertTrue(score >= 0.0 && score <= 1.0,
                    "Score must be between 0 and 1, got: " + score);
        }
    }

    @Test
    void scoresAreInDescendingOrder() {
        Task task = taskWith(new HashMap<>(Map.of(
                "queryText", "vector embeddings semantic search",
                "topK", 3
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertNotNull(documents);

        for (int i = 1; i < documents.size(); i++) {
            double prev = ((Number) documents.get(i - 1).get("score")).doubleValue();
            double curr = ((Number) documents.get(i).get("score")).doubleValue();
            assertTrue(prev >= curr,
                    "Scores should be in descending order, but position " + (i - 1)
                            + " (" + prev + ") < position " + i + " (" + curr + ")");
        }
    }

    @Test
    void returnsTotalSearchedAsPositiveNumber() {
        Task task = taskWith(new HashMap<>(Map.of(
                "queryText", "Conductor orchestration",
                "topK", 3
        )));
        TaskResult result = worker.execute(task);

        Object totalSearched = result.getOutputData().get("totalSearched");
        assertNotNull(totalSearched, "Output must include totalSearched");
        assertTrue(((Number) totalSearched).intValue() > 0,
                "totalSearched must be a positive number");
    }

    @Test
    void failsOnMissingQuery() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("required"));
    }

    @Test
    void failsOnBlankQuery() {
        Task task = taskWith(new HashMap<>(Map.of("queryText", "   ")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("required"));
    }

    @Test
    void failsOnQueryWithNoResults() {
        // Use a query that has zero overlap with the knowledge base
        Task task = taskWith(new HashMap<>(Map.of(
                "queryText", "xyzzy plugh qwerty zxcvb",
                "topK", 3
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("No documents matched"));
        assertNotNull(result.getOutputData().get("totalSearched"));
        assertNotNull(result.getOutputData().get("threshold"));
    }

    @Test
    void failsOnInvalidTopK() {
        Task task = taskWith(new HashMap<>(Map.of(
                "queryText", "Conductor",
                "topK", 0
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("topK"));
    }

    @Test
    void usesQuestionWhenQueryTextMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Conductor orchestration?",
                "topK", 2
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertNotNull(documents);
        assertFalse(documents.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
