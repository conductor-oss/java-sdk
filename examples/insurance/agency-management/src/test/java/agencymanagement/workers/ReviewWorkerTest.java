package agencymanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReviewWorkerTest {

    @Test
    void testReviewWorker() {
        ReviewWorker worker = new ReviewWorker();
        assertEquals("agm_review", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("agentId", "AGT-702", "performance", "data"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("exceeds-expectations", result.getOutputData().get("rating"));
    }
}
