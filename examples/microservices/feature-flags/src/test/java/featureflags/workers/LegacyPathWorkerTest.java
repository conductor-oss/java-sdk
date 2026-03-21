package featureflags.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LegacyPathWorkerTest {
    private final LegacyPathWorker worker = new LegacyPathWorker();

    @Test void taskDefName() { assertEquals("ff_legacy_path", worker.getTaskDefName()); }

    @Test void executesLegacyPath() {
        TaskResult r = worker.execute(taskWith(Map.of("feature", "dark-mode")));
        assertEquals("legacy", r.getOutputData().get("path"));
        assertEquals("v1", r.getOutputData().get("uiVersion"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
