package performancereview.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SelfEvalWorkerTest {

    private final SelfEvalWorker worker = new SelfEvalWorker();

    @Test
    void taskDefName() {
        assertEquals("pfr_self_eval", worker.getTaskDefName());
    }

    @Test
    void returnsSelfEvaluation() {
        Task task = taskWith(Map.of("employeeId", "EMP-300", "reviewPeriod", "2023-H2"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4, result.getOutputData().get("rating"));
        assertNotNull(result.getOutputData().get("strengths"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
