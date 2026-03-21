package twiliointegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WaitResponseWorkerTest {

    private final WaitResponseWorker worker = new WaitResponseWorker();

    @Test
    void taskDefName() {
        assertEquals("twl_wait_response", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("messageSid", "SM123", "toNumber", "+15551234567"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("YES", result.getOutputData().get("responseBody"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
