package loyaltyrewards.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EarnPointsWorkerTest {
    @Test void testExecute() {
        EarnPointsWorker worker = new EarnPointsWorker();
        assertEquals("lyr_earn_points", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("customerId", "CUST-42", "orderTotal", 65));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(65, result.getOutputData().get("earned"));
    }
}
