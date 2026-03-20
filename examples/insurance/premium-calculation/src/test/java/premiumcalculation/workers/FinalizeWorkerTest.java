package premiumcalculation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FinalizeWorkerTest {

    @Test
    void testFinalizeWorker() {
        FinalizeWorker worker = new FinalizeWorker();
        assertEquals("pmc_finalize", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("adjustedPremium", "1530", "policyType", "auto"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1530, result.getOutputData().get("finalPremium"));
    }
}
