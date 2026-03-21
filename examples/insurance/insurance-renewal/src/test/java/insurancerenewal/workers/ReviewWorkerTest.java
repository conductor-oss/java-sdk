package insurancerenewal.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReviewWorkerTest {

    @Test
    void testReviewWorker() {
        ReviewWorker worker = new ReviewWorker();
        assertEquals("irn_review", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("policyId", "POL-878", "claimHistory", "1"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.35, result.getOutputData().get("riskScore"));
    }
}
