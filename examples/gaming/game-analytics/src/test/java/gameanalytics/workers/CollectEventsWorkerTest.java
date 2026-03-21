package gameanalytics.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CollectEventsWorkerTest {
    @Test void testExecute() {
        CollectEventsWorker w = new CollectEventsWorker();
        assertEquals("gan_collect_events", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("gameId", "GAME-01", "dateRange", "test"));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
