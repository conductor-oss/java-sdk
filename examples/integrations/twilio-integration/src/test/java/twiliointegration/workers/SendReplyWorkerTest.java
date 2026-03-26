package twiliointegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SendReplyWorkerTest {

    private final SendReplyWorker worker = new SendReplyWorker();

    @Test
    void taskDefName() {
        assertEquals("twl_send_reply", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("to", "+15551234567", "from", "+15559832143", "body", "Confirmed"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("messageSid"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
