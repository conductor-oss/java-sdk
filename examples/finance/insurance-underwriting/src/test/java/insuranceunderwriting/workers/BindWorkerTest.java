package insuranceunderwriting.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class BindWorkerTest {
    private final BindWorker worker = new BindWorker();
    @Test void taskDefName() { assertEquals("uw_bind", worker.getTaskDefName()); }
    @Test void bindsOnAccept() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("decision", "accept", "applicantName", "Test", "premium", 600.0)));
        TaskResult r = worker.execute(t);
        assertEquals(true, r.getOutputData().get("bound"));
        assertNotEquals("N/A", r.getOutputData().get("policyNumber"));
    }
    @Test void noPolicyOnDecline() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("decision", "decline", "applicantName", "Test")));
        TaskResult r = worker.execute(t);
        assertEquals(false, r.getOutputData().get("bound"));
        assertEquals("N/A", r.getOutputData().get("policyNumber"));
    }
}
