package stakeholderreporting.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class FormatWorkerTest {
    private final FormatWorker w = new FormatWorker();
    @Test void taskDefName() { assertEquals("shr_format", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("projectId","PROJ-42","period","2026-W10","updates","[]","summary","{}","report","{}")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
