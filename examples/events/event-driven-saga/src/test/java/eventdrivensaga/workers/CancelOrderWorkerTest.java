package eventdrivensaga.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CancelOrderWorkerTest {
    private final CancelOrderWorker worker = new CancelOrderWorker();

    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("orderId", "ORD-001", "amount", 99.99,
                "customerId", "CUST-001", "paymentId", "PAY-001")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
