package clinicaldecision.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GatherDataWorkerTest {
    private final GatherDataWorker w = new GatherDataWorker();
    @Test void taskDefName() { assertEquals("cds_gather_data", w.getTaskDefName()); }
    @Test void gathers() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "condition", "cardiovascular_risk")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("clinicalData"));
    }
}
