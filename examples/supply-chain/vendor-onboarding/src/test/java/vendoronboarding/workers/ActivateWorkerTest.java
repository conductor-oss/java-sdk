package vendoronboarding.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActivateWorkerTest {

    private final ActivateWorker worker = new ActivateWorker();

    @Test
    void taskDefName() {
        assertEquals("von_activate", worker.getTaskDefName());
    }

    @Test
    void activatesVendor() {
        Task task = taskWith(Map.of("vendorId", "VND-001", "approved", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("active"));
        assertNotNull(result.getOutputData().get("activatedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
