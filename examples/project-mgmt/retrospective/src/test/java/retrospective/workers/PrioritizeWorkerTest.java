package retrospective.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PrioritizeWorkerTest {
    private final PrioritizeWorker worker = new PrioritizeWorker();

    @Test void taskDefName() { assertEquals("rsp_prioritize", worker.getTaskDefName()); }

    @Test void prioritizesItems() {
        Task task = taskWith(Map.of("sprintId", "SPR-42", "categories", "test categories"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("priorities"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
