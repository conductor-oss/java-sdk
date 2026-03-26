package referralmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleReferralWorkerTest {
    private final ScheduleReferralWorker w = new ScheduleReferralWorker();
    @Test void taskDefName() { assertEquals("ref_schedule", w.getTaskDefName()); }
    @Test void schedules() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("referralId", "R1", "patientId", "P1", "specialistId", "DR-1")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("appointmentDate"));
    }
}
