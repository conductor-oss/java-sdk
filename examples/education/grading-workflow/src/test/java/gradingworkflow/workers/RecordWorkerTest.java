package gradingworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RecordWorkerTest {
    private final RecordWorker worker = new RecordWorker();

    @Test void taskDefName() { assertEquals("grd_record", worker.getTaskDefName()); }

    @Test void recordsScore() {
        Task task = taskWith(Map.of("studentId", "STU-001", "courseId", "CS-101", "finalScore", 88));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
