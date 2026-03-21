package featureflags.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DefaultPathWorkerTest {
    private final DefaultPathWorker worker = new DefaultPathWorker();

    @Test void taskDefName() { assertEquals("ff_default_path", worker.getTaskDefName()); }

    @Test void executesDefaultPath() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals("default", r.getOutputData().get("path"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
