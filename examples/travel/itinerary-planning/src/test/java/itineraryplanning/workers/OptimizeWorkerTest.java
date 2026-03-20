package itineraryplanning.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class OptimizeWorkerTest {
    private final OptimizeWorker w = new OptimizeWorker();
    @Test void taskDefName() { assertEquals("itp_optimize", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("travelerId","TRV-200","destination","Chicago","days","3","options","{}","itinerary","{}","bookingIds","[]")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
