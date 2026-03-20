package apitestgeneration.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class GenerateTestsWorkerTest {
    private final GenerateTestsWorker worker = new GenerateTestsWorker();
    @Test void taskDefName() { assertEquals("atg_generate_tests", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("endpoints", java.util.List.of(java.util.Map.of("path","/users","method","GET","params",java.util.List.of())));
        t.setInputData(input);
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
