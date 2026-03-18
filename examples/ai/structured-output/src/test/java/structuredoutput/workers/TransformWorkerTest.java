package structuredoutput.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformWorkerTest {

    private final TransformWorker worker = new TransformWorker();

    @Test
    void taskDefName() {
        assertEquals("so_transform", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void addsValidatedAndTimestamp() {
        Map<String, Object> data = new HashMap<>(Map.of(
                "name", "Acme Corp",
                "industry", "Technology"
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "validated", true,
                "data", data
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> finalResult = (Map<String, Object>) result.getOutputData().get("final");
        assertNotNull(finalResult);
        assertEquals("Acme Corp", finalResult.get("name"));
        assertEquals("Technology", finalResult.get("industry"));
        assertEquals(true, finalResult.get("_validated"));
        assertEquals("2026-03-07T00:00:00Z", finalResult.get("_timestamp"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void preservesOriginalDataFields() {
        Map<String, Object> data = new HashMap<>(Map.of(
                "name", "Test Co",
                "founded", 2020,
                "employees", 50
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "validated", true,
                "data", data
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> finalResult = (Map<String, Object>) result.getOutputData().get("final");
        assertEquals("Test Co", finalResult.get("name"));
        assertEquals(2020, finalResult.get("founded"));
        assertEquals(50, finalResult.get("employees"));
        // Enriched fields are added
        assertEquals(true, finalResult.get("_validated"));
        assertEquals("2026-03-07T00:00:00Z", finalResult.get("_timestamp"));
        // Original 3 + 2 enrichment fields = 5
        assertEquals(5, finalResult.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void outputKeyIsFinal() {
        Map<String, Object> data = new HashMap<>(Map.of("key", "value"));

        Task task = taskWith(new HashMap<>(Map.of(
                "validated", true,
                "data", data
        )));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("final"));
        Map<String, Object> finalResult = (Map<String, Object>) result.getOutputData().get("final");
        assertNotNull(finalResult);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
