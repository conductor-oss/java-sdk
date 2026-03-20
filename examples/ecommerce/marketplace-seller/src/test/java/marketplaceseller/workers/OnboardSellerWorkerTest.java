package marketplaceseller.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OnboardSellerWorkerTest {
    private final OnboardSellerWorker w = new OnboardSellerWorker();
    @Test void taskDefName() { assertEquals("mkt_onboard_seller", w.getTaskDefName()); }
    @Test void returnsDocuments() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("sellerId", "S1", "businessName", "Test Co", "category", "electronics")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("documents") instanceof Map);
    }
}
