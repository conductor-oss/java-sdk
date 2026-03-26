package liveops.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CloseWorkerTest {
    @Test void testExecute() {
        CloseWorker w = new CloseWorker();
        assertEquals("lop_close", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("eventId", "EVT-748"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("event"));
    }
}
