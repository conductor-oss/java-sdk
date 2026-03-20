package qualityinspection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RecordWorkerTest {
    private final RecordWorker worker = new RecordWorker();
    @Test void taskDefName() { assertEquals("qi_record", worker.getTaskDefName()); }

    @Test void recordsResult() {
        Task task = taskWith(Map.of("batchId", "B-001", "product", "Bearings", "result", "pass"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("recorded"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
