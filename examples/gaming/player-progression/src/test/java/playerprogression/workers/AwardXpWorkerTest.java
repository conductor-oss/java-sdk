package playerprogression.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AwardXpWorkerTest {
    @Test void testExecute() {
        AwardXpWorker w = new AwardXpWorker();
        assertEquals("ppg_award_xp", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("playerId", "P-042", "xpEarned", 500));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(5300, r.getOutputData().get("totalXp"));
    }
}
