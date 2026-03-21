package selfcorrection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateCodeWorkerTest {

    private final GenerateCodeWorker worker = new GenerateCodeWorker();

    @Test
    void taskDefName() {
        assertEquals("sc_generate_code", worker.getTaskDefName());
    }

    @Test
    void generatesCodeForRequirement() {
        Task task = taskWith(Map.of("requirement", "Write a fibonacci function"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("code"));
    }

    @Test
    void generatedCodeContainsFibonacci() {
        Task task = taskWith(Map.of("requirement", "Write a fibonacci function"));
        TaskResult result = worker.execute(task);

        String code = (String) result.getOutputData().get("code");
        assertTrue(code.contains("fibonacci"));
    }

    @Test
    void generatedCodeContainsRecursion() {
        Task task = taskWith(Map.of("requirement", "Write a fibonacci function"));
        TaskResult result = worker.execute(task);

        String code = (String) result.getOutputData().get("code");
        assertTrue(code.contains("fibonacci(n-1)"));
        assertTrue(code.contains("fibonacci(n-2)"));
    }

    @Test
    void generatedCodeContainsBaseCase() {
        Task task = taskWith(Map.of("requirement", "Write a fibonacci function"));
        TaskResult result = worker.execute(task);

        String code = (String) result.getOutputData().get("code");
        assertTrue(code.contains("n <= 1"));
    }

    @Test
    void handlesEmptyRequirement() {
        Map<String, Object> input = new HashMap<>();
        input.put("requirement", "");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("code"));
    }

    @Test
    void handlesNullRequirement() {
        Map<String, Object> input = new HashMap<>();
        input.put("requirement", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("code"));
    }

    @Test
    void handlesMissingRequirement() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("code"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
