package budgetapproval.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ReviewBudgetWorkerTest {
    private final ReviewBudgetWorker worker = new ReviewBudgetWorker();
    @Test void taskDefName() { assertEquals("bgt_review_budget", worker.getTaskDefName()); }
    @Test void approvesUnder50k() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("amount", 45000)));
        TaskResult r = worker.execute(t);
        assertEquals("approve", r.getOutputData().get("decision"));
    }
    @Test void revisesOver50k() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("amount", 75000)));
        TaskResult r = worker.execute(t);
        assertEquals("revise", r.getOutputData().get("decision"));
    }
}
