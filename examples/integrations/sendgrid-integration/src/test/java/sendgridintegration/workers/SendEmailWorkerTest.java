package sendgridintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SendEmailWorkerTest {

    private final SendEmailWorker worker = new SendEmailWorker();

    @Test
    void taskDefName() {
        assertEquals("sgd_send_email", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("to", "alice@example.com", "subject", "Welcome", "htmlBody", "<p>Hi</p>"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("messageId"));
        assertEquals(true, result.getOutputData().get("delivered"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
