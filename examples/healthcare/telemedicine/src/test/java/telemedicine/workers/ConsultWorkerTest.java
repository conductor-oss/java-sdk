package telemedicine.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConsultWorkerTest {
    private final ConsultWorker w = new ConsultWorker();
    @Test void taskDefName() { assertEquals("tlm_consult", w.getTaskDefName()); }
    @Test void returnsDiagnosis() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("visitId", "V1", "patientId", "P1", "reason", "cough", "connectionId", "C1")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("diagnosis"));
        assertEquals(true, r.getOutputData().get("followUpNeeded"));
    }
}
