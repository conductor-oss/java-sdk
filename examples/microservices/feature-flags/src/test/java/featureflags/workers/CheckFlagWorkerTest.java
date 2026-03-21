package featureflags.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CheckFlagWorkerTest {
    private final CheckFlagWorker worker = new CheckFlagWorker();

    @Test void taskDefName() { assertEquals("ff_check_flag", worker.getTaskDefName()); }

    @Test void enabledFeatureReturnsEnabled() {
        Task t = taskWith(Map.of("userId", "u1", "featureName", "new-checkout-ui"));
        TaskResult r = worker.execute(t);
        assertEquals("enabled", r.getOutputData().get("flagStatus"));
        assertEquals(100, r.getOutputData().get("rolloutPercent"));
    }

    @Test void disabledFeatureReturnsDisabled() {
        Task t = taskWith(Map.of("userId", "u1", "featureName", "dark-mode"));
        TaskResult r = worker.execute(t);
        assertEquals("disabled", r.getOutputData().get("flagStatus"));
    }

    @Test void handlesMissingInputs() {
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(Map.of())).getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
