package vendoronboarding.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApproveWorkerTest {

    private final ApproveWorker worker = new ApproveWorker();

    @Test
    void taskDefName() {
        assertEquals("von_approve", worker.getTaskDefName());
    }

    @Test
    void approvesHighScore() {
        Task task = taskWith(Map.of("vendorId", "VND-001", "score", 88));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test
    void rejectsLowScore() {
        Task task = taskWith(Map.of("vendorId", "VND-001", "score", 50));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("approved"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
