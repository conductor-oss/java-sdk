package performancereview.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ManagerEvalWorkerTest {

    private final ManagerEvalWorker worker = new ManagerEvalWorker();

    @Test
    void taskDefName() {
        assertEquals("pfr_manager_eval", worker.getTaskDefName());
    }

    @Test
    void returnsManagerEvaluation() {
        Task task = taskWith(Map.of("employeeId", "EMP-300", "managerId", "MGR-50"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4.2, result.getOutputData().get("rating"));
        assertNotNull(result.getOutputData().get("feedback"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
