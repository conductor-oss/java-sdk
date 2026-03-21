package freightmanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class TrackWorkerTest {
    @Test void taskDefName() { assertEquals("frm_track", new TrackWorker().getTaskDefName()); }
    @Test void completes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("origin","Detroit","destination","Houston","weight",2500,"carrier","Express","bookingId","FRT-001","rate",6250.0,"invoiceId","INV-001")));
        TaskResult r = new TrackWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
