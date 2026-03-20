package labresults.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class LabNotifyWorkerTest {
    private final LabNotifyWorker w = new LabNotifyWorker();
    @Test void taskDefName() { assertEquals("lab_notify", w.getTaskDefName()); }
    @Test void notifiesViaPortal() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("orderId", "O1", "patientId", "P1", "reportId", "R1", "critical", false)));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("notified"));
        assertEquals("portal", r.getOutputData().get("channel"));
    }
}
