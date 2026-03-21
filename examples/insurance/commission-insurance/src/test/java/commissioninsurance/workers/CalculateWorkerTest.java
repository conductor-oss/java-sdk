package commissioninsurance.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CalculateWorkerTest {

    @Test
    void testCalculateWorker() {
        CalculateWorker worker = new CalculateWorker();
        assertEquals("cin_calculate", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("agentId", "AGT-789", "premiumAmount", "3200"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(480.0, result.getOutputData().get("commission"));
    }
}
