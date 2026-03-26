package qualityinspection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TestWorkerTest {
    private final TestWorker worker = new TestWorker();
    @Test void taskDefName() { assertEquals("qi_test", worker.getTaskDefName()); }

    @Test void passesWithNoDefects() {
        Task task = taskWith(Map.of("samples", List.of(Map.of("id", "S-1"))));
        TaskResult r = worker.execute(task);
        assertEquals("pass", r.getOutputData().get("result"));
        assertEquals(0, r.getOutputData().get("defects"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
