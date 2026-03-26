package basicrag;

import basicrag.workers.EmbedQueryWorker;
import basicrag.workers.GenerateAnswerWorker;
import basicrag.workers.SearchVectorsWorker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying worker-to-worker data flow in the basic RAG pipeline.
 * Tests the SearchVectors -> GenerateAnswer data contract without requiring an API key,
 * and the full embed -> search -> generate pipeline when an API key is available.
 */
class BasicRagIntegrationTest {

    @Test
    void searchOutputFeedsIntoGenerateInput() {
        // Step 1: Run search worker
        SearchVectorsWorker searchWorker = new SearchVectorsWorker();
        Task searchTask = new Task();
        searchTask.setStatus(Task.Status.IN_PROGRESS);
        searchTask.setInputData(new HashMap<>(Map.of(
                "queryText", "What is Conductor orchestration?",
                "topK", 3
        )));
        TaskResult searchResult = searchWorker.execute(searchTask);
        assertEquals(TaskResult.Status.COMPLETED, searchResult.getStatus(),
                "Search step must succeed");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents =
                (List<Map<String, Object>>) searchResult.getOutputData().get("documents");
        assertNotNull(documents, "Search must produce documents");
        assertFalse(documents.isEmpty(), "Search must produce at least one document");

        // Step 2: Verify the output format is compatible with GenerateAnswerWorker's expected input
        // GenerateAnswerWorker expects context as List<Map<String, Object>> with "text" field
        for (Map<String, Object> doc : documents) {
            assertNotNull(doc.get("text"), "Each search result must have a 'text' field for generation");
            assertFalse(((String) doc.get("text")).isBlank(), "Document text must not be blank");
        }

        // Step 3: Attempt to feed into GenerateAnswerWorker (will fail without API key,
        // but we validate the input contract is satisfied)
        GenerateAnswerWorker generateWorker = new GenerateAnswerWorker();
        Task generateTask = new Task();
        generateTask.setStatus(Task.Status.IN_PROGRESS);
        generateTask.setInputData(new HashMap<>(Map.of(
                "question", "What is Conductor orchestration?",
                "context", documents
        )));

        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key != null && !key.isBlank()) {
            // Full integration: actually call the LLM
            TaskResult generateResult = generateWorker.execute(generateTask);
            assertEquals(TaskResult.Status.COMPLETED, generateResult.getStatus());
            assertNotNull(generateResult.getOutputData().get("answer"));
            assertFalse(((String) generateResult.getOutputData().get("answer")).isBlank());
        } else {
            // Without API key, we verify the worker throws IllegalStateException
            // (not a validation error), confirming inputs are valid
            assertThrows(IllegalStateException.class, () -> generateWorker.execute(generateTask),
                    "With valid inputs but no API key, worker should throw IllegalStateException");
        }
    }

    @Test
    void searchResultDocumentsHaveRequiredFieldsForDownstream() {
        SearchVectorsWorker searchWorker = new SearchVectorsWorker();
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of(
                "queryText", "RAG retrieval augmented generation",
                "topK", 5
        )));
        TaskResult result = searchWorker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs =
                (List<Map<String, Object>>) result.getOutputData().get("documents");

        for (Map<String, Object> doc : docs) {
            // Fields needed by GenerateAnswerWorker
            assertTrue(doc.containsKey("text"), "Missing 'text' field");
            assertTrue(doc.containsKey("id"), "Missing 'id' field");
            assertTrue(doc.containsKey("score"), "Missing 'score' field");

            double score = ((Number) doc.get("score")).doubleValue();
            assertTrue(score >= 0.05, "All returned documents should be above threshold");
        }
    }

    @Test
    void emptySearchResultsPreventsGeneration() {
        // A query that yields no results from search
        SearchVectorsWorker searchWorker = new SearchVectorsWorker();
        Task searchTask = new Task();
        searchTask.setStatus(Task.Status.IN_PROGRESS);
        searchTask.setInputData(new HashMap<>(Map.of(
                "queryText", "xyzzy plugh qwerty zxcvb",
                "topK", 3
        )));
        TaskResult searchResult = searchWorker.execute(searchTask);
        assertEquals(TaskResult.Status.FAILED, searchResult.getStatus(),
                "Search with no matches must fail explicitly");

        // Verify that if empty results were passed to generate, it would also fail
        GenerateAnswerWorker generateWorker = new GenerateAnswerWorker();
        Task generateTask = new Task();
        generateTask.setStatus(Task.Status.IN_PROGRESS);
        generateTask.setInputData(new HashMap<>(Map.of(
                "question", "xyzzy plugh qwerty",
                "context", List.of()
        )));
        TaskResult generateResult = generateWorker.execute(generateTask);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, generateResult.getStatus(),
                "GenerateAnswerWorker must reject empty context");
    }
}
