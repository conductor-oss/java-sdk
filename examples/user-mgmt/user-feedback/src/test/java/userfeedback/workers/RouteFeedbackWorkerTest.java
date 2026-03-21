package userfeedback.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RouteFeedbackWorkerTest {
    private final RouteFeedbackWorker w = new RouteFeedbackWorker();
    @Test void taskDefName() { assertEquals("ufb_route", w.getTaskDefName()); }
    @Test void routesBugToEngineering() {
        TaskResult r = w.execute(t(Map.of("category", "bug", "priority", "high")));
        assertEquals("engineering", r.getOutputData().get("assignedTeam"));
    }
    @Test void routesFeatureToProduct() {
        TaskResult r = w.execute(t(Map.of("category", "feature_request", "priority", "medium")));
        assertEquals("product", r.getOutputData().get("assignedTeam"));
    }
    @Test void routesGeneralToSupport() {
        TaskResult r = w.execute(t(Map.of("category", "general", "priority", "medium")));
        assertEquals("support", r.getOutputData().get("assignedTeam"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
