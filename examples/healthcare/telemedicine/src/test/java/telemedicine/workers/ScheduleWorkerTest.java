package telemedicine.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleWorkerTest {
    private final ScheduleWorker w = new ScheduleWorker();
    @Test void taskDefName() { assertEquals("tlm_schedule", w.getTaskDefName()); }
    @Test void schedulesVisit() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("visitId", "V1", "patientId", "P1", "providerId", "D1", "reason", "cough")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("sessionUrl"));
    }
}
