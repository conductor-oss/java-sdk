package leaderboardupdate.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CollectScoresWorkerTest {
    @Test void testExecute() {
        CollectScoresWorker worker = new CollectScoresWorker();
        assertEquals("lbu_collect_scores", worker.getTaskDefName());
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(Map.of("gameId", "GAME-01"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("scores"));
    }
}
