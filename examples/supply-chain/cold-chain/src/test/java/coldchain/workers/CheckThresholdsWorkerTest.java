package coldchain.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CheckThresholdsWorkerTest {
    @Test void taskDefName() { assertEquals("cch_check_thresholds", new CheckThresholdsWorker().getTaskDefName()); }
    @Test void completes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("shipmentId","COLD-001","product","Pharma","minTemp",2,"maxTemp",8,"currentTemp",3.2,"status","ok")));
        TaskResult r = new CheckThresholdsWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
