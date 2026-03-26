package nonprofitdonation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ReceiveWorkerTest {
    @Test void testExecute() {
        ReceiveWorker w = new ReceiveWorker(); assertEquals("don_receive", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("donorName", "Jane Smith", "amount", 250));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
