package qualityinspection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HandleFailWorkerTest {
    private final HandleFailWorker worker = new HandleFailWorker();
    @Test void taskDefName() { assertEquals("qi_handle_fail", worker.getTaskDefName()); }

    @Test void quarantinesBatch() {
        Task task = taskWith(Map.of("batchId", "B-001", "defects", 5));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("quarantine", r.getOutputData().get("action"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
