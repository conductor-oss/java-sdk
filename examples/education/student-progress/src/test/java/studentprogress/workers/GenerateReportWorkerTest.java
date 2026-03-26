package studentprogress.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GenerateReportWorkerTest {
    private final GenerateReportWorker worker = new GenerateReportWorker();
    @Test void taskDefName() { assertEquals("spr_generate_report", worker.getTaskDefName()); }
    @Test void generatesReport() {
        Task task = taskWith(Map.of("studentId", "STU-001", "analysis", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("generated"));
        assertEquals("PDF", result.getOutputData().get("format"));
    }
    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
