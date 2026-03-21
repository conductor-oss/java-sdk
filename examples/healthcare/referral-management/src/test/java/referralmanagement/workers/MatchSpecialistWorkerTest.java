package referralmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MatchSpecialistWorkerTest {
    private final MatchSpecialistWorker w = new MatchSpecialistWorker();
    @Test void taskDefName() { assertEquals("ref_match_specialist", w.getTaskDefName()); }
    @Test void matches() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("specialty", "Ortho", "patientId", "P1", "insurancePlan", "BCBS")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("specialistName"));
    }
}
