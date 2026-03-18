package understandingworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SendConfirmationWorkerTest {

    private final SendConfirmationWorker worker = new SendConfirmationWorker();

    @Test
    void taskDefName() {
        assertEquals("send_confirmation", worker.getTaskDefName());
    }

    @Test
    void sendsConfirmationSuccessfully() {
        Task task = taskWith(Map.of(
                "customerEmail", "alice@example.com",
                "total", 1144.77,
                "orderId", "ORD-1001"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("emailSent"));
        assertEquals("alice@example.com", result.getOutputData().get("recipient"));
    }

    @Test
    void recipientMatchesInputEmail() {
        Task task = taskWith(Map.of(
                "customerEmail", "bob@test.org",
                "total", 50.00,
                "orderId", "ORD-2002"
        ));
        TaskResult result = worker.execute(task);

        assertEquals("bob@test.org", result.getOutputData().get("recipient"));
    }

    @Test
    void handlesNullEmail() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerEmail", null);
        input.put("total", 100.0);
        input.put("orderId", "ORD-3003");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("emailSent"));
        assertEquals("unknown", result.getOutputData().get("recipient"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
