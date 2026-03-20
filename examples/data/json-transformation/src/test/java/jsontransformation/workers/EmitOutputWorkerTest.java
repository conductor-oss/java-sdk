package jsontransformation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmitOutputWorkerTest {

    private final EmitOutputWorker worker = new EmitOutputWorker();

    @Test
    void taskDefName() {
        assertEquals("jt_emit_output", worker.getTaskDefName());
    }

    @Test
    void emitsValidRecord() {
        Map<String, Object> finalRecord = Map.of(
                "identity", Map.of("id", "C-9001", "name", "Jane Doe"),
                "contact", Map.of("email", "jane@example.com"),
                "account", Map.of("type", "PREMIUM"));
        Task task = taskWith(Map.of("finalRecord", finalRecord, "isValid", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("emitted"));
        assertNotNull(result.getOutputData().get("record"));
    }

    @Test
    void recordMatchesFinalRecord() {
        Map<String, Object> finalRecord = Map.of("identity", Map.of("id", "C-100"));
        Task task = taskWith(Map.of("finalRecord", finalRecord, "isValid", true));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> record = (Map<String, Object>) result.getOutputData().get("record");
        @SuppressWarnings("unchecked")
        Map<String, Object> identity = (Map<String, Object>) record.get("identity");
        assertEquals("C-100", identity.get("id"));
    }

    @Test
    void emittedIsAlwaysTrue() {
        Task task = taskWith(Map.of("finalRecord", Map.of(), "isValid", false));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("emitted"));
    }

    @Test
    void handlesInvalidRecord() {
        Task task = taskWith(Map.of("finalRecord", Map.of(), "isValid", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("emitted"));
    }

    @Test
    void handlesNullFinalRecord() {
        Map<String, Object> input = new HashMap<>();
        input.put("finalRecord", null);
        input.put("isValid", false);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNull(result.getOutputData().get("record"));
        assertEquals(true, result.getOutputData().get("emitted"));
    }

    @Test
    void handlesMissingInputKeys() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("emitted"));
    }

    @Test
    void handlesNullIsValid() {
        Map<String, Object> input = new HashMap<>();
        input.put("finalRecord", Map.of("identity", Map.of("id", "C-200")));
        input.put("isValid", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("emitted"));
        assertNotNull(result.getOutputData().get("record"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
