package ragcitation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyCitationsWorkerTest {

    private final VerifyCitationsWorker worker = new VerifyCitationsWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_verify_citations", worker.getTaskDefName());
    }

    @Test
    void verifiesAllCitationsWhenDocIdsMatch() {
        List<Map<String, Object>> citations = new ArrayList<>();
        citations.add(new HashMap<>(Map.of("marker", "[1]", "docId", "doc-1")));
        citations.add(new HashMap<>(Map.of("marker", "[2]", "docId", "doc-2")));

        List<Map<String, Object>> documents = new ArrayList<>();
        documents.add(new HashMap<>(Map.of("id", "doc-1", "text", "text 1")));
        documents.add(new HashMap<>(Map.of("id", "doc-2", "text", "text 2")));

        Task task = taskWith(new HashMap<>(Map.of("citations", citations, "documents", documents)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("verifiedCount"));
        assertEquals(true, result.getOutputData().get("allVerified"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(2, results.size());
        assertEquals(true, results.get(0).get("verified"));
        assertEquals(true, results.get(1).get("verified"));
    }

    @Test
    void detectsUnverifiedCitations() {
        List<Map<String, Object>> citations = new ArrayList<>();
        citations.add(new HashMap<>(Map.of("marker", "[1]", "docId", "doc-1")));
        citations.add(new HashMap<>(Map.of("marker", "[2]", "docId", "doc-99")));

        List<Map<String, Object>> documents = new ArrayList<>();
        documents.add(new HashMap<>(Map.of("id", "doc-1", "text", "text 1")));

        Task task = taskWith(new HashMap<>(Map.of("citations", citations, "documents", documents)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("verifiedCount"));
        assertEquals(false, result.getOutputData().get("allVerified"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(true, results.get(0).get("verified"));
        assertEquals(false, results.get(1).get("verified"));
    }

    @Test
    void resultsHaveRequiredFields() {
        List<Map<String, Object>> citations = new ArrayList<>();
        citations.add(new HashMap<>(Map.of("marker", "[1]", "docId", "doc-1")));

        List<Map<String, Object>> documents = new ArrayList<>();
        documents.add(new HashMap<>(Map.of("id", "doc-1", "text", "text")));

        Task task = taskWith(new HashMap<>(Map.of("citations", citations, "documents", documents)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");

        for (Map<String, Object> r : results) {
            assertNotNull(r.get("marker"));
            assertNotNull(r.get("docId"));
            assertNotNull(r.get("verified"));
        }
    }

    @Test
    void handlesNullCitations() {
        List<Map<String, Object>> documents = new ArrayList<>();
        documents.add(new HashMap<>(Map.of("id", "doc-1", "text", "text")));

        Task task = taskWith(new HashMap<>(Map.of("documents", documents)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("verifiedCount"));
        assertEquals(true, result.getOutputData().get("allVerified"));
    }

    @Test
    void handlesNullDocuments() {
        List<Map<String, Object>> citations = new ArrayList<>();
        citations.add(new HashMap<>(Map.of("marker", "[1]", "docId", "doc-1")));

        Task task = taskWith(new HashMap<>(Map.of("citations", citations)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("verifiedCount"));
        assertEquals(false, result.getOutputData().get("allVerified"));
    }

    @Test
    void handlesEmptyInputs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("verifiedCount"));
        assertEquals(true, result.getOutputData().get("allVerified"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
