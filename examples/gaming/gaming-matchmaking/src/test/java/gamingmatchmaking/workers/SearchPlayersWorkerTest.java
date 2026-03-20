package gamingmatchmaking.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class SearchPlayersWorkerTest {
    @Test void testExecute() {
        SearchPlayersWorker worker = new SearchPlayersWorker();
        assertEquals("gmm_search_players", worker.getTaskDefName());
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(Map.of("gameMode", "ranked", "region", "NA"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("candidates"));
    }
}
