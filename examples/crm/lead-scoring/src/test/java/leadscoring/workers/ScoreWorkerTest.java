package leadscoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ScoreWorkerTest {
    @Test void taskDefName() { assertEquals("ls_score", new ScoreWorker().getTaskDefName()); }

    @Test void highScoreForEnterpriseTech() {
        ScoreWorker w = new ScoreWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("signals", Map.of(
                "companySize", "enterprise", "industry", "technology",
                "pageViews", 30, "emailOpens", 5, "demoRequested", true))));
        TaskResult r = w.execute(t);
        assertTrue(((Number) r.getOutputData().get("totalScore")).intValue() >= 80);
    }

    @Test void lowScoreForUnknown() {
        ScoreWorker w = new ScoreWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("signals", Map.of(
                "companySize", "unknown", "industry", "unknown",
                "pageViews", 1, "emailOpens", 0, "demoRequested", false))));
        TaskResult r = w.execute(t);
        assertTrue(((Number) r.getOutputData().get("totalScore")).intValue() < 40);
    }
}
