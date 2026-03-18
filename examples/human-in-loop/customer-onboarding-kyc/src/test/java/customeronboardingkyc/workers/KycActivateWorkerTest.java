package customeronboardingkyc.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class KycActivateWorkerTest {
    @Test void taskDefName() { assertEquals("kyc_activate", new KycActivateWorker().getTaskDefName()); }

    @Test void activatesCustomer() {
        KycActivateWorker w = new KycActivateWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("customerId", "C-1001", "customerName", "Alice Smith")));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("activated"));
        assertEquals("C-1001", r.getOutputData().get("customerId"));
    }
}
