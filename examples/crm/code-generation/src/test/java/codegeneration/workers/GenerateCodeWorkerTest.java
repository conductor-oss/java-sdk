package codegeneration.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class GenerateCodeWorkerTest {
    private final GenerateCodeWorker worker = new GenerateCodeWorker();
    @Test void taskDefName() { assertEquals("cdg_generate_code", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("requirements", "Build API"); input.put("language", "java"); input.put("framework", "spring");
        input.put("parsed", Map.of()); input.put("code", "// code"); input.put("testCases", List.of(Map.of("name", "t1")));
        t.setInputData(input);
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(t).getStatus());
    }
}
