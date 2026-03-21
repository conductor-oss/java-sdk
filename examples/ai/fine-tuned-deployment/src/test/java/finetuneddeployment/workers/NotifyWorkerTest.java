package finetuneddeployment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotifyWorkerTest {

    private final NotifyWorker worker = new NotifyWorker();

    @Test
    void taskDefName() {
        assertEquals("ftd_notify", worker.getTaskDefName());
    }

    @Test
    void notifiesCompletion() {
        Task task = taskWith(new HashMap<>(Map.of(
                "modelId", "support-bot-v3",
                "reason", "Pipeline complete",
                "prodEndpoint", "https://api.models.internal/support-bot-v3")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("notified"));
    }

    @Test
    void notifiesFailure() {
        Task task = taskWith(new HashMap<>(Map.of(
                "modelId", "support-bot-v3",
                "reason", "Acceptance tests failed")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("notified"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
