package ehrintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class QueryPatientWorkerTest {
    private final QueryPatientWorker w = new QueryPatientWorker();
    @Test void taskDefName() { assertEquals("ehr_query_patient", w.getTaskDefName()); }
    @Test void returnsRecords() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "sourceSystem", "all")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("records"));
    }
}
