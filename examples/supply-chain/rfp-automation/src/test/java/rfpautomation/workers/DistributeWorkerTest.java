package rfpautomation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class DistributeWorkerTest {
    @Test void taskDefName() { assertEquals("rfp_distribute", new DistributeWorker().getTaskDefName()); }
    @Test void completes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("projectTitle","Test","requirements",List.of("a"),"deadline","2024-01-01","rfpId","RFP-001","proposals",List.of(Map.of("vendor","A","score",90,"price",100000)),"topCandidate","A")));
        TaskResult r = new DistributeWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
