package salvagerecovery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AssessDamageWorkerTest {

    @Test
    void testAssessDamageWorker() {
        AssessDamageWorker worker = new AssessDamageWorker();
        assertEquals("slv_assess_damage", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("claimId", "CLM-701", "vehicleId", "VIN-98321"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("totalLoss"));
        assertEquals(4200, result.getOutputData().get("salvageValue"));
    }
}
