package sendgridintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TrackOpensWorkerTest {

    private final TrackOpensWorker worker = new TrackOpensWorker();

    @Test
    void taskDefName() {
        assertEquals("sgd_track_opens", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("messageId", "sg-123", "campaignId", "camp-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("trackingEnabled"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
