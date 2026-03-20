package remotemonitoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CollectVitalsWorkerTest {
    private final CollectVitalsWorker w = new CollectVitalsWorker();
    @Test void taskDefName() { assertEquals("rpm_collect_vitals", w.getTaskDefName()); }
    @Test void collectsVitals() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "deviceId", "D1")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("vitals"));
    }
}
