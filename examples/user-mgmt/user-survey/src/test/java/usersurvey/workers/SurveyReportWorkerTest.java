package usersurvey.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class SurveyReportWorkerTest {
    private final SurveyReportWorker w = new SurveyReportWorker();
    @Test void taskDefName() { assertEquals("usv_report", w.getTaskDefName()); }
    @Test void generatesReport() {
        TaskResult r = w.execute(t(Map.of("analysis", Map.of(), "surveyId", "SRV-123")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("reportUrl").toString().contains("SRV-123"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
