package failureworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotifyFailureWorkerTest {

    private final NotifyFailureWorker worker = new NotifyFailureWorker();

    @Test
    void taskDefName() {
        assertEquals("fw_notify_failure", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsNotifiedTrue() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("notified"));
    }

    @Test
    void returnsNotificationMessage() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals("Failure notification sent", result.getOutputData().get("message"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
