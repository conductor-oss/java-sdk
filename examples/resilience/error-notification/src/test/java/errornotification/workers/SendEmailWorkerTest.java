package errornotification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SendEmailWorkerTest {

    @Test
    void taskDefName() {
        SendEmailWorker worker = new SendEmailWorker();
        assertEquals("en_send_email", worker.getTaskDefName());
    }

    @Test
    void sendsEmailWithProvidedRecipient() {
        SendEmailWorker worker = new SendEmailWorker();
        Task task = taskWith(Map.of("to", "admin@example.com"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
        assertEquals("admin@example.com", result.getOutputData().get("to"));
    }

    @Test
    void sendsEmailWithDefaultRecipient() {
        SendEmailWorker worker = new SendEmailWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
        assertEquals("oncall@example.com", result.getOutputData().get("to"));
    }

    @Test
    void outputContainsSentAndTo() {
        SendEmailWorker worker = new SendEmailWorker();
        Task task = taskWith(Map.of("to", "test@example.com"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("sent"));
        assertTrue(result.getOutputData().containsKey("to"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
