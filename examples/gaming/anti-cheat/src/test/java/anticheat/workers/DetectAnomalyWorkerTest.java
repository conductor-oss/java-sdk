package anticheat.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class DetectAnomalyWorkerTest {
    @Test void testCleanVerdict() {
        DetectAnomalyWorker w = new DetectAnomalyWorker();
        assertEquals("ach_detect_anomaly", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("metrics", Map.of("aimAccuracy", 0.45)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("clean", r.getOutputData().get("verdict"));
    }
}
