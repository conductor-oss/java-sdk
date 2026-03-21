package remotemonitoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AnalyzeTrendsWorkerTest {
    private final AnalyzeTrendsWorker w = new AnalyzeTrendsWorker();
    @Test void taskDefName() { assertEquals("rpm_analyze_trends", w.getTaskDefName()); }
    @Test void detectsAlerts() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> vitals = new HashMap<>();
        vitals.put("heartRate", 92);
        vitals.put("bloodPressure", Map.of("systolic", 155, "diastolic", 95));
        vitals.put("oxygenSaturation", 94);
        vitals.put("glucose", 185);
        vitals.put("temperature", 98.8);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "vitals", vitals)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("alert", r.getOutputData().get("status"));
    }
}
