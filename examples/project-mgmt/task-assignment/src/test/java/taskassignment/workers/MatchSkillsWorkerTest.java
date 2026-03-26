package taskassignment.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class MatchSkillsWorkerTest {
    private final MatchSkillsWorker w = new MatchSkillsWorker();
    @Test void taskDefName() { assertEquals("tas_match_skills", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("taskTitle","Build search","requiredSkills","JS","priority","high","skills","[]","candidate","{}","assignee","Alice")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
