package impactreporting.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CollectDataWorkerTest {
    @Test void testExecute() { CollectDataWorker w = new CollectDataWorker(); assertEquals("ipr_collect_data", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("programName", "Test", "year", 2025));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus()); }
}
