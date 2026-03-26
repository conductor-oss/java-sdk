package clickstreamanalytics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeJourneysWorkerTest {

    private final AnalyzeJourneysWorker worker = new AnalyzeJourneysWorker();

    @Test
    void taskDefName() {
        assertEquals("ck_analyze_journeys", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void detectsConversions() {
        List<Map<String, Object>> sessions = List.of(
                Map.of("userId", "U1", "clicks", 3, "pages", List.of("/home", "/products", "/checkout"), "duration", 90),
                Map.of("userId", "U2", "clicks", 2, "pages", List.of("/home", "/products"), "duration", 30));
        Task task = taskWith(Map.of("sessions", sessions));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("50.0%", result.getOutputData().get("conversionRate"));

        Map<String, Object> analysis = (Map<String, Object>) result.getOutputData().get("analysis");
        assertEquals(1, analysis.get("converted"));
    }

    @Test
    void detectsBounces() {
        List<Map<String, Object>> sessions = List.of(
                Map.of("userId", "U1", "clicks", 1, "pages", List.of("/home"), "duration", 0));
        Task task = taskWith(Map.of("sessions", sessions));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) result.getOutputData().get("analysis");
        assertEquals(1, analysis.get("bounced"));
    }

    @Test
    void handlesEmptySessions() {
        Task task = taskWith(Map.of("sessions", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("0.0%", result.getOutputData().get("conversionRate"));
        assertEquals("none", result.getOutputData().get("topJourney"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
