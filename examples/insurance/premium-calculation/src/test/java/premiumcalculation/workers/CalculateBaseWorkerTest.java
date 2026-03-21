package premiumcalculation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CalculateBaseWorkerTest {

    @Test
    void testCalculateBaseWorker() {
        CalculateBaseWorker worker = new CalculateBaseWorker();
        assertEquals("pmc_calculate_base", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("factors", "data", "coverageAmount", "100000"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1800, result.getOutputData().get("basePremium"));
    }
}
