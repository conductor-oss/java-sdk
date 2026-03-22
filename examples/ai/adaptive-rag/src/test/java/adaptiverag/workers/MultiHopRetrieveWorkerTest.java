package adaptiverag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MultiHopRetrieveWorkerTest {

    private final MultiHopRetrieveWorker worker = new MultiHopRetrieveWorker();

    @Test
    void taskDefName() {
        assertEquals("ar_mhop_ret", worker.getTaskDefName());
    }

    @Test
    void returnsFourDocuments() {
        Task task = taskWith(new HashMap<>(Map.of("question", "Compare Conductor and Temporal")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertNotNull(documents);
        assertEquals(4, documents.size());
    }

    @Test
    void documentsCoverTwoHops() {
        Task task = taskWith(new HashMap<>(Map.of("question", "Compare Conductor and Temporal")));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("hops"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");
        long hop1Count = documents.stream().filter(d -> Integer.valueOf(1).equals(d.get("hop"))).count();
        long hop2Count = documents.stream().filter(d -> Integer.valueOf(2).equals(d.get("hop"))).count();
        assertEquals(2, hop1Count);
        assertEquals(2, hop2Count);
    }

    @Test
    void documentsContainRelevantContent() {
        Task task = taskWith(new HashMap<>(Map.of("question", "Compare Conductor and Temporal")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertTrue(documents.stream().anyMatch(d -> d.get("text").toString().contains("Conductor")));
        assertTrue(documents.stream().anyMatch(d -> d.get("text").toString().contains("Temporal")));
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
        Task task = taskWith(new HashMap<>(Map.of("question", "")));
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
