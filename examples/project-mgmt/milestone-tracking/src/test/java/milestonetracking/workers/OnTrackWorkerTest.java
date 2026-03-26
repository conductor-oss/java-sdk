package milestonetracking.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class OnTrackWorkerTest {
    private final OnTrackWorker w = new OnTrackWorker();
    @Test void taskDefName() { assertEquals("mst_on_track", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("milestoneId","MS-Q1","projectName","Alpha","progress","{}","status","at_risk")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
