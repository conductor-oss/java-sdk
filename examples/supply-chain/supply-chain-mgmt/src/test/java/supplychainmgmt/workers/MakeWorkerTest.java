package supplychainmgmt.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MakeWorkerTest {

    private final MakeWorker worker = new MakeWorker();

    @Test
    void taskDefName() {
        assertEquals("scm_make", worker.getTaskDefName());
    }

    @Test
    void manufacturesProduct() {
        Task task = taskWith(Map.of("product", "Sensor", "quantity", 500));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("BATCH-651-001", result.getOutputData().get("batchId"));
        assertEquals(500, result.getOutputData().get("unitsProduced"));
    }

    @Test
    void handlesZeroQuantity() {
        Task task = taskWith(Map.of("product", "Test", "quantity", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("unitsProduced"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
