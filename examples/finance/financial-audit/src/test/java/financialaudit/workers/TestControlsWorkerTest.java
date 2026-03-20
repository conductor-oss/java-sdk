package financialaudit.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class TestControlsWorkerTest {
    private final TestControlsWorker worker = new TestControlsWorker();
    @Test void taskDefName() { assertEquals("fau_test_controls", worker.getTaskDefName()); }
    @Test void testsControls() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("evidenceItems", 47)));
        TaskResult r = worker.execute(t);
        assertEquals("adequate", r.getOutputData().get("controlEffectiveness"));
        assertEquals(3, r.getOutputData().get("findingsCount"));
    }
}
