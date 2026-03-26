package regulatoryfiling.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ValidateWorkerTest {
    @Test void taskDefName() { assertEquals("rgf_validate", new ValidateWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("filingType", "10K", "entityName", "Corp", "jurisdiction", "US", "filingPackage", "pkg", "submissionId", "S1", "validated", "true", "trackingStatus", "ok")));
        TaskResult r = new ValidateWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("validated"));
    }
}
