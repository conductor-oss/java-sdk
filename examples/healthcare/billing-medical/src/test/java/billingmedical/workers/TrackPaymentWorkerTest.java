package billingmedical.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TrackPaymentWorkerTest {
    private final TrackPaymentWorker w = new TrackPaymentWorker();
    @Test void taskDefName() { assertEquals("mbl_track_payment", w.getTaskDefName()); }
    @Test void tracks() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("claimId", "CLM-1", "expectedPayment", 200.0)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("pending", r.getOutputData().get("paymentStatus"));
    }
}
