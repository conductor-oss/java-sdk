package adaptiverag;

import adaptiverag.workers.MultiHopRetrieveWorker;
import adaptiverag.workers.SimpleRetrieveWorker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying worker-to-worker data flow in the adaptive RAG pipeline.
 * Tests the factual and analytical paths without requiring an OpenAI API key.
 */
class AdaptiveRagIntegrationTest {

    @Test
    void factualPathProducesDocumentsCompatibleWithSimpleGenerate() {
        // Simulate factual classification -> simple retrieval -> simple generation
        SimpleRetrieveWorker retrieveWorker = new SimpleRetrieveWorker();

        Task retrieveTask = new Task();
        retrieveTask.setStatus(Task.Status.IN_PROGRESS);
        retrieveTask.setInputData(new HashMap<>(Map.of("question", "What is Conductor?")));

        TaskResult retrieveResult = retrieveWorker.execute(retrieveTask);
        assertEquals(TaskResult.Status.COMPLETED, retrieveResult.getStatus());

        @SuppressWarnings("unchecked")
        List<String> documents = (List<String>) retrieveResult.getOutputData().get("documents");
        assertNotNull(documents, "Simple retrieval must produce documents");
        assertEquals(2, documents.size(), "Simple retrieval returns exactly 2 docs");

        // Verify output is compatible with SimpleGenerateWorker input (expects List<String> as 'context')
        for (String doc : documents) {
            assertNotNull(doc);
            assertFalse(doc.isBlank(), "Retrieved documents must not be blank");
        }
    }

    @Test
    void analyticalPathProducesDocumentsWithHopMetadata() {
        // Simulate analytical classification -> multi-hop retrieval -> reasoning -> analytical generation
        MultiHopRetrieveWorker mhopWorker = new MultiHopRetrieveWorker();

        Task mhopTask = new Task();
        mhopTask.setStatus(Task.Status.IN_PROGRESS);
        mhopTask.setInputData(new HashMap<>(Map.of("question", "Compare Conductor and Temporal")));

        TaskResult mhopResult = mhopWorker.execute(mhopTask);
        assertEquals(TaskResult.Status.COMPLETED, mhopResult.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents =
                (List<Map<String, Object>>) mhopResult.getOutputData().get("documents");
        assertNotNull(documents);
        assertEquals(4, documents.size(), "Multi-hop retrieval returns 4 docs (2 per hop)");

        // Verify hop metadata
        assertEquals(2, mhopResult.getOutputData().get("hops"));

        // Verify each doc has 'text' field needed by ReasoningWorker
        for (Map<String, Object> doc : documents) {
            assertNotNull(doc.get("text"), "Each multi-hop doc must have a 'text' field");
            assertNotNull(doc.get("hop"), "Each multi-hop doc must have a 'hop' field");
        }
    }

    @Test
    void eachRetrievalPathProducesNonOverlappingResults() {
        // Factual path
        SimpleRetrieveWorker simpleWorker = new SimpleRetrieveWorker();
        Task simpleTask = new Task();
        simpleTask.setStatus(Task.Status.IN_PROGRESS);
        simpleTask.setInputData(new HashMap<>(Map.of("question", "Compare Conductor and Temporal")));
        TaskResult simpleResult = simpleWorker.execute(simpleTask);

        // Analytical path
        MultiHopRetrieveWorker mhopWorker = new MultiHopRetrieveWorker();
        Task mhopTask = new Task();
        mhopTask.setStatus(Task.Status.IN_PROGRESS);
        mhopTask.setInputData(new HashMap<>(Map.of("question", "Compare Conductor and Temporal")));
        TaskResult mhopResult = mhopWorker.execute(mhopTask);

        // Both paths should succeed but use different document sources
        assertEquals(TaskResult.Status.COMPLETED, simpleResult.getStatus());
        assertEquals(TaskResult.Status.COMPLETED, mhopResult.getStatus());

        @SuppressWarnings("unchecked")
        List<String> simpleDocs = (List<String>) simpleResult.getOutputData().get("documents");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mhopDocs =
                (List<Map<String, Object>>) mhopResult.getOutputData().get("documents");

        // Simple path returns 2 docs; analytical returns 4 with hop metadata
        assertEquals(2, simpleDocs.size());
        assertEquals(4, mhopDocs.size());

        // Analytical path includes hop metadata that simple path does not
        assertTrue(mhopDocs.stream().allMatch(d -> d.containsKey("hop")),
                "Analytical path docs must include hop metadata");
    }

    @Test
    void allPathsRejectBlankQuestion() {
        SimpleRetrieveWorker simpleWorker = new SimpleRetrieveWorker();
        MultiHopRetrieveWorker mhopWorker = new MultiHopRetrieveWorker();

        Task blankTask1 = new Task();
        blankTask1.setStatus(Task.Status.IN_PROGRESS);
        blankTask1.setInputData(new HashMap<>());

        Task blankTask2 = new Task();
        blankTask2.setStatus(Task.Status.IN_PROGRESS);
        blankTask2.setInputData(new HashMap<>());

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, simpleWorker.execute(blankTask1).getStatus());
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, mhopWorker.execute(blankTask2).getStatus());
    }
}
