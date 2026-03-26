package gradingworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GradeWorkerTest {
    private final GradeWorker worker = new GradeWorker();

    @Test void taskDefName() { assertEquals("grd_grade", worker.getTaskDefName()); }

    @Test void gradesSubmission() {
        Task task = taskWith(Map.of("submissionId", "SUB-001", "assignmentId", "HW-05"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(88, result.getOutputData().get("score"));
        assertNotNull(result.getOutputData().get("feedback"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
