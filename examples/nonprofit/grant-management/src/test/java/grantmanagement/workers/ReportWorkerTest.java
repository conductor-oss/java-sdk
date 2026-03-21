package grantmanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ReportWorkerTest {
    @Test void testExecute() {
        ReportWorker w = new ReportWorker(); assertEquals("gmt_report", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("grantId", "GRT-752", "organization", "Test"));
        assertNotNull(w.execute(t).getOutputData().get("grant"));
    }
}
