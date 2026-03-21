package changetracking.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ClassifyChangeWorkerTest {
    @Test void taskDefName() { assertEquals("chg_classify", new ClassifyChangeWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("filesChanged",8, "linesAdded",145, "linesRemoved",32, "resourceType","service")));
        TaskResult r = new ClassifyChangeWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("medium", r.getOutputData().get("riskLevel"));
        assertEquals("moderate", r.getOutputData().get("changeType"));
    }
}
