package summarizationpipeline.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class GenerateSummaryWorkerTest {
    private final GenerateSummaryWorker worker = new GenerateSummaryWorker();
    @Test void taskDefName() { assertEquals("sum_generate_summary", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("document", "test");
        input.put("sections", List.of(Map.of("title", "Intro", "wordCount", 100, "key", "test")));
        input.put("compressedSections", List.of()); input.put("maxLength", 100);
        t.setInputData(input);
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
