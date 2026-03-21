package remotemonitoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AlertActionWorkerTest {
    private final AlertActionWorker w = new AlertActionWorker();
    @Test void taskDefName() { assertEquals("rpm_alert_action", w.getTaskDefName()); }
    @Test void alertAction() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "vitals", Map.of(), "alerts", List.of("High BP"))));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("provider_notified", r.getOutputData().get("action"));
    }
}
