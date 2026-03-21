package seasonmanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CreateSeasonWorkerTest {
    @Test void testExecute() {
        CreateSeasonWorker w = new CreateSeasonWorker();
        assertEquals("smg_create_season", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("seasonNumber", 3, "theme", "Test"));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
