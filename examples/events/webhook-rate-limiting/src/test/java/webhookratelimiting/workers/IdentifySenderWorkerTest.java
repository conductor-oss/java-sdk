package webhookratelimiting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IdentifySenderWorkerTest {

    private final IdentifySenderWorker worker = new IdentifySenderWorker();

    @Test
    void taskDefName() {
        assertEquals("wl_identify_sender", worker.getTaskDefName());
    }

    @Test
    void identifiesSenderById() {
        Task task = taskWith(Map.of("senderId", "partner-api-xyz"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("partner-api-xyz", result.getOutputData().get("senderId"));
    }

    @Test
    void outputContainsIpAddress() {
        Task task = taskWith(Map.of("senderId", "partner-api-xyz"));
        TaskResult result = worker.execute(task);

        assertEquals("192.168.1.100", result.getOutputData().get("ip"));
    }

    @Test
    void preservesSenderIdInOutput() {
        Task task = taskWith(Map.of("senderId", "webhook-client-42"));
        TaskResult result = worker.execute(task);

        assertEquals("webhook-client-42", result.getOutputData().get("senderId"));
    }

    @Test
    void handlesNullSenderId() {
        Map<String, Object> input = new HashMap<>();
        input.put("senderId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("senderId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("senderId"));
        assertEquals("192.168.1.100", result.getOutputData().get("ip"));
    }

    @Test
    void handlesEmptyStringSenderId() {
        Task task = taskWith(Map.of("senderId", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("senderId"));
        assertEquals("192.168.1.100", result.getOutputData().get("ip"));
    }

    @Test
    void alwaysReturnsStaticIp() {
        Task task1 = taskWith(Map.of("senderId", "sender-a"));
        Task task2 = taskWith(Map.of("senderId", "sender-b"));
        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("ip"), result2.getOutputData().get("ip"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
