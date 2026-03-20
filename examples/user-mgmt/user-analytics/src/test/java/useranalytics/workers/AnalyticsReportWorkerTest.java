package useranalytics.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AnalyticsReportWorkerTest {
    private final AnalyticsReportWorker w = new AnalyticsReportWorker();
    @Test void taskDefName() { assertEquals("uan_report", w.getTaskDefName()); }
    @Test void generatesReport() { TaskResult r = w.execute(t(Map.of("metrics", Map.of()))); assertEquals(TaskResult.Status.COMPLETED, r.getStatus()); assertNotNull(r.getOutputData().get("dashboardUrl")); assertNotNull(r.getOutputData().get("generatedAt")); }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
