package selfrag.workers;

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
        assertEquals("sr_retrieve", worker.getTaskDefName());
    }

    @Test
    void returnsFourDocuments() {
        Task task = taskWith(new HashMap<>(Map.of("question", "What task types does Conductor support?")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs = (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertNotNull(docs);
        assertEquals(4, docs.size());
    }

    @Test
    void threeDocsRelevantOneIrrelevant() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs = (List<Map<String, Object>>) result.getOutputData().get("documents");
        long relevant = docs.stream()
                .filter(d -> ((Number) d.get("score")).doubleValue() >= 0.5)
                .count();
        assertEquals(3, relevant);

        long irrelevant = docs.stream()
                .filter(d -> ((Number) d.get("score")).doubleValue() < 0.5)
                .count();
        assertEquals(1, irrelevant);
    }

    @Test
    void irrelevantDocHasScore022() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs = (List<Map<String, Object>>) result.getOutputData().get("documents");
        Map<String, Object> irrelevant = docs.stream()
                .filter(d -> ((Number) d.get("score")).doubleValue() < 0.5)
                .findFirst().orElseThrow();
        assertEquals(0.22, ((Number) irrelevant.get("score")).doubleValue(), 0.001);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
