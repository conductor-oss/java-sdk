package serverlessorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SvlInvokeParseWorkerTest {

    private final SvlInvokeParseWorker worker = new SvlInvokeParseWorker();

    @Test
    void taskDefName() {
        assertEquals("svl_invoke_parse", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of(
                "functionArn", "arn:aws:lambda:us-east-1:123:function:parse-event",
                "eventId", "EVT-001",
                "payload", Map.of("action", "page_view")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsParsedData() {
        Task task = taskWith(Map.of("eventId", "EVT-002", "payload", Map.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) result.getOutputData().get("parsed");
        assertNotNull(parsed);
        assertEquals("click", parsed.get("type"));
        assertEquals("/products", parsed.get("page"));
        assertEquals("U-123", parsed.get("userId"));
    }

    @Test
    void outputContainsBilledMs() {
        Task task = taskWith(Map.of("eventId", "EVT-003", "payload", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(45, result.getOutputData().get("billedMs"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("payload", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("parsed"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void parsedDataHasThreeFields() {
        Task task = taskWith(Map.of("eventId", "EVT-005", "payload", Map.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) result.getOutputData().get("parsed");
        assertEquals(3, parsed.size());
    }

    @Test
    void differentEventIdsProduceSameOutput() {
        Task task1 = taskWith(Map.of("eventId", "EVT-A", "payload", Map.of()));
        Task task2 = taskWith(Map.of("eventId", "EVT-B", "payload", Map.of()));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("parsed"), r2.getOutputData().get("parsed"));
        assertEquals(r1.getOutputData().get("billedMs"), r2.getOutputData().get("billedMs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
