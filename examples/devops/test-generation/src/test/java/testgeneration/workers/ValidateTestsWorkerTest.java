package testgeneration.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ValidateTestsWorkerTest {
    private final ValidateTestsWorker worker = new ValidateTestsWorker();
    @Test void taskDefName() { assertEquals("tge_validate_tests", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("tests", List.of(Map.of("functionName", "foo", "testCases", List.of("test_foo_happy"))));
        input.put("language", "javascript");
        t.setInputData(input);
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
