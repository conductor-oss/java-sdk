package virtualeconomy.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class TransactionWorkerTest {
    @Test void testExecute() {
        TransactionWorker w = new TransactionWorker();
        assertEquals("vec_transaction", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("playerId", "P-042", "type", "earn", "amount", 500));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
