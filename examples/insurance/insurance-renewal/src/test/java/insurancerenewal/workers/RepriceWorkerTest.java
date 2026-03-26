package insurancerenewal.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RepriceWorkerTest {

    @Test
    void testRepriceWorker() {
        RepriceWorker worker = new RepriceWorker();
        assertEquals("irn_reprice", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("policyId", "POL-878", "riskScore", "0.35"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("renew", result.getOutputData().get("decision"));
        assertEquals(1200, result.getOutputData().get("newPremium"));
    }
}
