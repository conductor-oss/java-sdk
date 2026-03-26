package gradingworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SubmitWorkerTest {
    private final SubmitWorker worker = new SubmitWorker();

    @Test void taskDefName() { assertEquals("grd_submit", worker.getTaskDefName()); }

    @Test void submitsAssignment() {
        Task task = taskWith(Map.of("studentId", "STU-001", "assignmentId", "HW-05"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("submissionId"));
        assertEquals(true, result.getOutputData().get("onTime"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
