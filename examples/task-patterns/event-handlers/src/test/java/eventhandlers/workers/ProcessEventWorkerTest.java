package eventhandlers.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessEventWorkerTest {

    private final ProcessEventWorker worker = new ProcessEventWorker();

    @Test
    void taskDefName() {
        assertEquals("eh_process_event", worker.getTaskDefName());
    }

    @Test
    void processesEventWithTypeAndPayload() {
        Task task = taskWith(Map.of(
                "eventType", "order.created",
                "payload", Map.of("orderId", "ORD-123", "amount", 49.99)
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed order.created event", result.getOutputData().get("result"));
        assertEquals("order.created", result.getOutputData().get("eventType"));
    }

    @Test
    void returnsPayloadInOutput() {
        Map<String, Object> payload = Map.of("key", "value");
        Task task = taskWith(Map.of("eventType", "test.event", "payload", payload));
        TaskResult result = worker.execute(task);

        assertEquals(payload, result.getOutputData().get("payload"));
    }

    @Test
    void defaultsEventTypeWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed unknown event", result.getOutputData().get("result"));
        assertEquals("unknown", result.getOutputData().get("eventType"));
    }

    @Test
    void defaultsEventTypeWhenBlank() {
        Task task = taskWith(Map.of("eventType", "   "));
        TaskResult result = worker.execute(task);

        assertEquals("Processed unknown event", result.getOutputData().get("result"));
        assertEquals("unknown", result.getOutputData().get("eventType"));
    }

    @Test
    void defaultsPayloadWhenMissing() {
        Task task = taskWith(Map.of("eventType", "user.signup"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed user.signup event", result.getOutputData().get("result"));
        assertEquals("{}", result.getOutputData().get("payload"));
    }

    @Test
    void handlesVariousEventTypes() {
        for (String eventType : List.of("order.created", "user.signup", "payment.processed", "inventory.updated")) {
            Task task = taskWith(Map.of("eventType", eventType, "payload", Map.of()));
            TaskResult result = worker.execute(task);

            assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
            assertEquals("Processed " + eventType + " event", result.getOutputData().get("result"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }

    private static java.util.List<String> List_of(String... items) {
        return java.util.List.of(items);
    }
}
