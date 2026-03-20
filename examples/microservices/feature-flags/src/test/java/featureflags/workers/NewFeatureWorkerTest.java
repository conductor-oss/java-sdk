package featureflags.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NewFeatureWorkerTest {
    private final NewFeatureWorker worker = new NewFeatureWorker();

    @Test void taskDefName() { assertEquals("ff_new_feature", worker.getTaskDefName()); }

    @Test void executesNewPath() {
        TaskResult r = worker.execute(taskWith(Map.of("feature", "checkout")));
        assertEquals("new", r.getOutputData().get("path"));
        assertEquals("v2", r.getOutputData().get("uiVersion"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
