package marketplaceseller.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ManageOrdersWorkerTest {
    private final ManageOrdersWorker w = new ManageOrdersWorker();
    @Test void taskDefName() { assertEquals("mkt_manage_orders", w.getTaskDefName()); }
    @Test void activatesOrderManagement() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("sellerId", "S1", "storeId", "STORE-S1", "productCount", 12)));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("active"));
    }
}
