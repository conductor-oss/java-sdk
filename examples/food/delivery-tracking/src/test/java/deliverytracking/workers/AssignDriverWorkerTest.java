package deliverytracking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AssignDriverWorkerTest {
    @Test void testExecute() {
        AssignDriverWorker worker = new AssignDriverWorker();
        assertEquals("dlt_assign_driver", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("orderId", "ORD-733", "restaurantAddr", "123 Main St"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("DRV-55", result.getOutputData().get("driverId"));
    }
}
