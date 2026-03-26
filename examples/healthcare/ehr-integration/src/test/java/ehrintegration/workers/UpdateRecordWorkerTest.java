package ehrintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UpdateRecordWorkerTest {
    private final UpdateRecordWorker w = new UpdateRecordWorker();
    @Test void taskDefName() { assertEquals("ehr_update", w.getTaskDefName()); }
    @Test void updates() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "validatedRecord", Map.of("name", "Test"))));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("updated"));
    }
}
