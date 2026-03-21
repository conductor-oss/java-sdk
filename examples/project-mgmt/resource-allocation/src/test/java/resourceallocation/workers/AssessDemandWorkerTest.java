package resourceallocation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AssessDemandWorkerTest {
    private final AssessDemandWorker w = new AssessDemandWorker();
    @Test void taskDefName() { assertEquals("ral_assess_demand", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("projectId","PROJ-42","resourceType","developer","hoursNeeded","40","demand","{}","available","[]","allocation","{}")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
