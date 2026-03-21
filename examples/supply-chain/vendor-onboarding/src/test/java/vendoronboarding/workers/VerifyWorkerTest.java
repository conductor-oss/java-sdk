package vendoronboarding.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyWorkerTest {

    private final VerifyWorker worker = new VerifyWorker();

    @Test
    void taskDefName() {
        assertEquals("von_verify", worker.getTaskDefName());
    }

    @Test
    void verifiesVendor() {
        Task task = taskWith(Map.of("vendorId", "VND-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
        assertNotNull(result.getOutputData().get("checks"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
