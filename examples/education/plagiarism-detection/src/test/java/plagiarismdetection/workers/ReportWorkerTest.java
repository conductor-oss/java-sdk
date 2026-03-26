package plagiarismdetection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReportWorkerTest {
    private final ReportWorker worker = new ReportWorker();
    @Test void taskDefName() { assertEquals("plg_report", worker.getTaskDefName()); }
    @Test void generatesReport() {
        Task task = taskWith(Map.of("studentId", "STU-001", "assignmentId", "ESSAY-01", "verdict", "clean"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("generated"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
