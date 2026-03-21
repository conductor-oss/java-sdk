package qualityinspection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SampleWorkerTest {
    private final SampleWorker worker = new SampleWorker();
    @Test void taskDefName() { assertEquals("qi_sample", worker.getTaskDefName()); }

    @SuppressWarnings("unchecked")
    @Test void pullsSamples() {
        Task task = taskWith(Map.of("batchId", "B-001", "sampleSize", 5));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(5, ((List<?>) r.getOutputData().get("samples")).size());
        assertEquals(5, r.getOutputData().get("count"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
