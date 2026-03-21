package plagiarismdetection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SubmitWorkerTest {
    private final SubmitWorker worker = new SubmitWorker();
    @Test void taskDefName() { assertEquals("plg_submit", worker.getTaskDefName()); }
    @Test void submitsDocument() {
        Task task = taskWith(Map.of("studentId", "STU-001", "assignmentId", "ESSAY-01"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("submissionId"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
