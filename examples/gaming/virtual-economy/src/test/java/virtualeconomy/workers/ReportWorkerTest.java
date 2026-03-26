package virtualeconomy.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ReportWorkerTest {
    @Test void testExecute() {
        ReportWorker w = new ReportWorker();
        assertEquals("vec_report", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("playerId", "P-042"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("summary"));
    }
}
