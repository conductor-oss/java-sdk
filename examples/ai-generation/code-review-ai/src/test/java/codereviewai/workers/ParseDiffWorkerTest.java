package codereviewai.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ParseDiffWorkerTest {
    private final ParseDiffWorker worker = new ParseDiffWorker();
    @Test void taskDefName() { assertEquals("cra_parse_diff", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("prUrl", "https://github.com/test/pr/1"); input.put("diff", "+test");
        input.put("files", List.of("test.js")); input.put("linesChanged", 10);
        input.put("security", List.of(Map.of("severity", "high"))); input.put("quality", List.of());
        input.put("style", List.of());
        t.setInputData(input);
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(t).getStatus());
    }
}
