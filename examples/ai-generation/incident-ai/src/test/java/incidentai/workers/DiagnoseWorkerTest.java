package incidentai.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class DiagnoseWorkerTest {
    private final DiagnoseWorker worker = new DiagnoseWorker();
    @Test void taskDefName() { assertEquals("iai_diagnose", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("incidentDetails", java.util.Map.of("type","high_error_rate","errorRate",0.15,"affectedEndpoints",java.util.List.of("/api/checkout")));
        t.setInputData(input);
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
