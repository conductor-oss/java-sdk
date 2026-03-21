package marketplaceseller.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VerifySellerWorkerTest {
    private final VerifySellerWorker w = new VerifySellerWorker();
    @Test void taskDefName() { assertEquals("mkt_verify_seller", w.getTaskDefName()); }
    @Test void verifiesAndReturnsStoreId() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("sellerId", "S1", "documents", Map.of("taxId", "EIN-1"))));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("verified"));
        assertEquals("STORE-S1", r.getOutputData().get("storeId"));
    }
}
