package bulkuserimport.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ValidateRecordsWorkerTest {
    private final ValidateRecordsWorker w = new ValidateRecordsWorker();
    @Test void taskDefName() { assertEquals("bui_validate", w.getTaskDefName()); }
    @Test void validates() {
        TaskResult r = w.execute(t(Map.of("records", List.of(), "totalParsed", 1250)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(1238, r.getOutputData().get("validCount"));
        assertEquals(12, r.getOutputData().get("invalidCount"));
    }
    @Test void includesErrors() {
        TaskResult r = w.execute(t(Map.of("records", List.of(), "totalParsed", 1250)));
        assertNotNull(r.getOutputData().get("errors"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
