package iotsecurity.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class DetectVulnerabilitiesWorkerTest {
    private final DetectVulnerabilitiesWorker worker = new DetectVulnerabilitiesWorker();
    @Test void taskDefName() { assertEquals("ios_detect_vulnerabilities", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("vulnerabilities"));
        assertNotNull(r.getOutputData().get("vulnCount"));
    }
}
