package featureflags.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LogUsageWorkerTest {
    private final LogUsageWorker worker = new LogUsageWorker();

    @Test void taskDefName() { assertEquals("ff_log_usage", worker.getTaskDefName()); }

    @Test void logsUsage() {
        Task t = taskWith(Map.of("feature", "new-checkout-ui", "flagStatus", "enabled"));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("logged"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
