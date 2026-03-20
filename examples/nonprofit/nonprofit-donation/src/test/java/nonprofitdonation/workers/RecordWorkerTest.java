package nonprofitdonation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class RecordWorkerTest {
    @Test void testExecute() {
        RecordWorker w = new RecordWorker(); assertEquals("don_record", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("donorName", "Jane Smith", "amount", 250, "transactionId", "TXN-D751"));
        TaskResult r = w.execute(t); assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("donation"));
    }
}
