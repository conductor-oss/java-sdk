package compliancenonprofit.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class SubmitWorkerTest {
    @Test void testExecute() { SubmitWorker w = new SubmitWorker(); assertEquals("cnp_submit", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("ein", "12-3456789"));
        TaskResult r = w.execute(t); assertNotNull(r.getOutputData().get("submission")); }
}
