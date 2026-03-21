package mentalhealth.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AssessWorkerTest {
    private final AssessWorker w = new AssessWorker();
    @Test void taskDefName() { assertEquals("mh_assess", w.getTaskDefName()); }
    @Test void assessesPatient() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "intakeData", Map.of("symptoms", List.of("sadness")))));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("diagnosis"));
        assertEquals(14, r.getOutputData().get("phq9Score"));
    }
}
