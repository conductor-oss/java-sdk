package usersurvey.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AnalyzeSurveyWorkerTest {
    private final AnalyzeSurveyWorker w = new AnalyzeSurveyWorker();
    @Test void taskDefName() { assertEquals("usv_analyze", w.getTaskDefName()); }
    @SuppressWarnings("unchecked")
    @Test void analyzesSurvey() {
        TaskResult r = w.execute(t(Map.of("responses", List.of())));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        Map<String, Object> analysis = (Map<String, Object>) r.getOutputData().get("analysis");
        assertEquals(4.2, analysis.get("avgSatisfaction"));
        assertNotNull(analysis.get("topThemes"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
