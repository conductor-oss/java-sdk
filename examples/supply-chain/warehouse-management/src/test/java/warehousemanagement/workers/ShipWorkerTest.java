package warehousemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ShipWorkerTest {
    @Test void taskDefName() { assertEquals("wm_ship", new ShipWorker().getTaskDefName()); }
    @Test void shipsPackage() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("packageId","PKG-001","shippingMethod","express")));
        TaskResult r = new ShipWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("TRK-657-XYZ", r.getOutputData().get("trackingNumber"));
    }
}
