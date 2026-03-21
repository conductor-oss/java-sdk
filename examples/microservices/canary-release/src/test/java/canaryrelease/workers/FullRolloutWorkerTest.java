package canaryrelease.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FullRolloutWorkerTest {
    private final FullRolloutWorker worker = new FullRolloutWorker();

    @Test void taskDefName() { assertEquals("cy_full_rollout", worker.getTaskDefName()); }

    @Test void completesRollout() {
        Task task = taskWith(Map.of("appName", "user-service", "version", "3.1.0"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("success", r.getOutputData().get("status"));
        assertEquals(true, r.getOutputData().get("complete"));
        assertEquals(100, r.getOutputData().get("trafficPercent"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
