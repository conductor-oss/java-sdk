package prreviewai.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AnalyzeChangesWorkerTest {
    private final AnalyzeChangesWorker worker = new AnalyzeChangesWorker();
    @Test void taskDefName() { assertEquals("prr_analyze_changes", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("diff", java.util.Map.of("files",java.util.List.of("a.js"),"additions",10,"deletions",2));
        t.setInputData(input);
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
