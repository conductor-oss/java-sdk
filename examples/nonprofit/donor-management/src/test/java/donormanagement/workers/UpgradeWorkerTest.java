package donormanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class UpgradeWorkerTest {
    @Test void testExecute() { UpgradeWorker w = new UpgradeWorker(); assertEquals("dnr_upgrade", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("donorId", "DNR-755", "currentLevel", "mid-level"));
        assertNotNull(w.execute(t).getOutputData().get("donor")); }
}
