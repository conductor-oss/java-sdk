package mentalhealth.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TreatmentPlanWorkerTest {
    private final TreatmentPlanWorker w = new TreatmentPlanWorker();
    @Test void taskDefName() { assertEquals("mh_treatment_plan", w.getTaskDefName()); }
    @Test void createsPlan() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "diagnosis", "MDD", "severity", "moderate", "provider", "Dr. X")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("treatmentPlan"));
    }
}
