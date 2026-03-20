package gamingmatchmaking.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class StartWorkerTest {
    @Test void testExecute() {
        StartWorker worker = new StartWorker();
        assertEquals("gmm_start", worker.getTaskDefName());
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(Map.of("lobbyId", "LOBBY-3301"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("game"));
    }
}
