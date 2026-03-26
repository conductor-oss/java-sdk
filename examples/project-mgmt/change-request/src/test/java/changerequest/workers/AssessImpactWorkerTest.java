package changerequest.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AssessImpactWorkerTest {
    private final AssessImpactWorker w = new AssessImpactWorker();
    @Test void taskDefName() { assertEquals("chr_assess_impact", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("changeId","CR-101","description","Add mobile","details","{}","impact","{}","approval","{}","implementation","{}")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
