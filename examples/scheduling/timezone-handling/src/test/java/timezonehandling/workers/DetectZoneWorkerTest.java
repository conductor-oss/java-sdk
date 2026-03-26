package timezonehandling.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DetectZoneWorkerTest {
    @Test void taskDefName() { assertEquals("tz_detect_zone", new DetectZoneWorker().getTaskDefName()); }
    @Test void detectsTimezone() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("userId", "u1")));
        TaskResult r = new DetectZoneWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("Asia/Tokyo", r.getOutputData().get("timezone"));
    }
}
