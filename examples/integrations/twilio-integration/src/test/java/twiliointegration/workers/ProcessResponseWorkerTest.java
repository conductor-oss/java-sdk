package twiliointegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessResponseWorkerTest {

    private final ProcessResponseWorker worker = new ProcessResponseWorker();

    @Test
    void taskDefName() {
        assertEquals("twl_process_response", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("responseBody", "YES", "originalMessage", "Confirm?"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("confirm", result.getOutputData().get("intent"));
        assertNotNull(result.getOutputData().get("replyMessage"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
