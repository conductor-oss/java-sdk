package projectclosure.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReviewDeliverablesWorkerTest {
    private final ReviewDeliverablesWorker worker = new ReviewDeliverablesWorker();

    @Test void taskDefName() { assertEquals("pcl_review_deliverables", worker.getTaskDefName()); }

    @Test void reviewsDeliverables() {
        Task task = taskWith(Map.of("projectId", "PRJ-909", "projectName", "Cloud Migration"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("deliverables"));
        assertEquals(true, result.getOutputData().get("allComplete"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
