package remotemonitoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NormalActionWorkerTest {
    private final NormalActionWorker w = new NormalActionWorker();
    @Test void taskDefName() { assertEquals("rpm_normal_action", w.getTaskDefName()); }
    @Test void normalAction() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "vitals", Map.of("heartRate", 72))));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("continue_monitoring", r.getOutputData().get("action"));
    }
}
