package bulkuserimport.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ReportWorkerTest {
    private final ReportWorker w = new ReportWorker();
    @Test void taskDefName() { assertEquals("bui_report", w.getTaskDefName()); }
    @Test void generatesReport() {
        TaskResult r = w.execute(t(Map.of("parsed", 1250, "valid", 1238, "inserted", 1238)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("reportUrl").toString().startsWith("https://"));
        assertNotNull(r.getOutputData().get("summary"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
