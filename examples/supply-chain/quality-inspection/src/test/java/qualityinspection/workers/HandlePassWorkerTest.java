package qualityinspection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HandlePassWorkerTest {
    private final HandlePassWorker worker = new HandlePassWorker();
    @Test void taskDefName() { assertEquals("qi_handle_pass", worker.getTaskDefName()); }

    @Test void releasesBatch() {
        Task task = taskWith(Map.of("batchId", "B-001"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("release", r.getOutputData().get("action"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
