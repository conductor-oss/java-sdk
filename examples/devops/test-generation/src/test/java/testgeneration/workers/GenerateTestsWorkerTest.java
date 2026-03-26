package testgeneration.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class GenerateTestsWorkerTest {
    private final GenerateTestsWorker worker = new GenerateTestsWorker();
    @Test void taskDefName() { assertEquals("tge_generate_tests", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("functions", List.of(Map.of("name", "foo", "params", List.of("a"), "lines", 5)));
        input.put("framework", "jest");
        t.setInputData(input);
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("tests"));
    }
}
