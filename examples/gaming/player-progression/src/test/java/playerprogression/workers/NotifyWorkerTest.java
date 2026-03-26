package playerprogression.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class NotifyWorkerTest {
    @Test void testExecute() {
        NotifyWorker w = new NotifyWorker();
        assertEquals("ppg_notify", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("playerId", "P-042", "rewards", List.of("Gold Shield")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
