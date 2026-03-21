package telemedicine.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PrescribeWorkerTest {
    private final PrescribeWorker w = new PrescribeWorker();
    @Test void taskDefName() { assertEquals("tlm_prescribe", w.getTaskDefName()); }
    @Test void prescribes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("visitId", "V1", "patientId", "P1", "diagnosis", "URI")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("prescription"));
        assertEquals(true, r.getOutputData().get("sentToPharmacy"));
    }
}
