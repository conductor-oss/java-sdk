package governmentpermit.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReviewWorkerTest {

    @Test
    void testReviewWorker() {
        ReviewWorker worker = new ReviewWorker();
        assertEquals("gvp_review", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("application", Map.of("id", "PRM-001")));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("approve", result.getOutputData().get("decision"));
    }
}
