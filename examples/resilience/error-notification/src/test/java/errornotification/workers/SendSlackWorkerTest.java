package errornotification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SendSlackWorkerTest {

    @Test
    void taskDefName() {
        SendSlackWorker worker = new SendSlackWorker();
        assertEquals("en_send_slack", worker.getTaskDefName());
    }

    @Test
    void sendsSlackWithProvidedChannel() {
        SendSlackWorker worker = new SendSlackWorker();
        Task task = taskWith(Map.of("channel", "#engineering"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
        assertEquals("#engineering", result.getOutputData().get("channel"));
    }

    @Test
    void sendsSlackWithDefaultChannel() {
        SendSlackWorker worker = new SendSlackWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
        assertEquals("#alerts", result.getOutputData().get("channel"));
    }

    @Test
    void outputContainsSentAndChannel() {
        SendSlackWorker worker = new SendSlackWorker();
        Task task = taskWith(Map.of("channel", "#ops"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("sent"));
        assertTrue(result.getOutputData().containsKey("channel"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
