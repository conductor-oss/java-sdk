package adaptiverag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SimpleRetrieveWorkerTest {

    private final SimpleRetrieveWorker worker = new SimpleRetrieveWorker();

    @Test
    void taskDefName() {
        assertEquals("ar_simple_ret", worker.getTaskDefName());
    }

    @Test
    void returnsTwoDocuments() {
        Task task = taskWith(new HashMap<>(Map.of("question", "What is Conductor?")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<String> documents = (List<String>) result.getOutputData().get("documents");
        assertNotNull(documents);
        assertEquals(2, documents.size());
    }

    @Test
    void documentsContainRelevantContent() {
        Task task = taskWith(new HashMap<>(Map.of("question", "What is Conductor?")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> documents = (List<String>) result.getOutputData().get("documents");
        // At least one document should mention Conductor
        assertTrue(documents.stream().anyMatch(d -> d.contains("Conductor")),
                "At least one document should mention Conductor for a Conductor query");
    }

    @Test
    void relevantQueryRanksMatchingDocsFirst() {
        Task task = taskWith(new HashMap<>(Map.of("question", "Orkes managed cloud enterprise")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> documents = (List<String>) result.getOutputData().get("documents");
        assertTrue(documents.get(0).contains("Orkes"),
                "An Orkes query should rank the Orkes document first");
    }

    @Test
    void failsOnMissingQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("question"));
    }

    @Test
    void failsOnBlankQuestion() {
        Task task = taskWith(new HashMap<>(Map.of("question", "   ")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("question"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
