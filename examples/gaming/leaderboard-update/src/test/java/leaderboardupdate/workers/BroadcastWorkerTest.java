package leaderboardupdate.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class BroadcastWorkerTest {
    @Test void testExecute() {
        BroadcastWorker worker = new BroadcastWorker();
        assertEquals("lbu_broadcast", worker.getTaskDefName());
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(Map.of("leaderboardId", "LB-742"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("leaderboard"));
    }
}
