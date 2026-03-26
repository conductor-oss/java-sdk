package configmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VerifyConfigWorkerTest {
    private final VerifyConfigWorker worker = new VerifyConfigWorker();

    @Test void taskDefName() { assertEquals("cf_verify_config", worker.getTaskDefName()); }

    @Test void completesSuccessfully() {
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("configSource", "consul", "environment", "prod", "schema", "v2", "deploymentId", "d1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void handlesMissingInputs() {
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>());
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(t).getStatus());
    }
}
