package tooluseconditional.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterWorkerTest {

    private final InterpreterWorker worker = new InterpreterWorker();

    @Test
    void taskDefName() {
        assertEquals("tc_interpreter", worker.getTaskDefName());
    }

    @Test
    void generatesPythonCodeByDefault() {
        Task task = taskWith(Map.of("codeRequest", "Write a fibonacci function", "language", "python"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String code = (String) result.getOutputData().get("code");
        assertNotNull(code);
        assertTrue(code.contains("def fibonacci"));
        assertEquals("python", result.getOutputData().get("language"));
    }

    @Test
    void generatesJavaScriptCode() {
        Task task = taskWith(Map.of("codeRequest", "Write a fibonacci function", "language", "javascript"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String code = (String) result.getOutputData().get("code");
        assertNotNull(code);
        assertTrue(code.contains("function fibonacci"));
        assertEquals("javascript", result.getOutputData().get("language"));
    }

    @Test
    void returnsExecutionOutput() {
        Task task = taskWith(Map.of("codeRequest", "fibonacci", "language", "python"));
        TaskResult result = worker.execute(task);

        assertEquals("fibonacci(10) = 55", result.getOutputData().get("executionOutput"));
    }

    @Test
    void returnsToolUsedInterpreter() {
        Task task = taskWith(Map.of("codeRequest", "sorting algorithm"));
        TaskResult result = worker.execute(task);

        assertEquals("interpreter", result.getOutputData().get("toolUsed"));
    }

    @Test
    void answerContainsCodeRequest() {
        Task task = taskWith(Map.of("codeRequest", "Write a binary search", "language", "python"));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("Write a binary search"));
    }

    @Test
    void defaultsLanguageToPython() {
        Map<String, Object> input = new HashMap<>();
        input.put("codeRequest", "sort a list");
        input.put("language", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("python", result.getOutputData().get("language"));
        String code = (String) result.getOutputData().get("code");
        assertTrue(code.contains("def fibonacci"));
    }

    @Test
    void handlesNullCodeRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("codeRequest", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("code"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
