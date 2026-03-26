package lastmiledelivery.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class DeliverWorkerTest {
    @Test void taskDefName() { assertEquals("lmd_deliver", new DeliverWorker().getTaskDefName()); }
    @Test void completes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("orderId","ORD-001","address","123 Main St","driverId","DRV-42","route","via I-90","deliveryStatus","delivered")));
        TaskResult r = new DeliverWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
