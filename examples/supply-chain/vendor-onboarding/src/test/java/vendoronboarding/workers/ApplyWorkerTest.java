package vendoronboarding.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplyWorkerTest {

    private final ApplyWorker worker = new ApplyWorker();

    @Test
    void taskDefName() {
        assertEquals("von_apply", worker.getTaskDefName());
    }

    @Test
    void createsApplication() {
        Task task = taskWith(Map.of("vendorName", "Acme", "category", "electronics", "country", "US"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("VND-653-001", result.getOutputData().get("vendorId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
