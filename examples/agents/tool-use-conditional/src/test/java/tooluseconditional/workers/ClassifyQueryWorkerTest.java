package tooluseconditional.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClassifyQueryWorkerTest {

    private final ClassifyQueryWorker worker = new ClassifyQueryWorker();

    @Test
    void taskDefName() {
        assertEquals("tc_classify_query", worker.getTaskDefName());
    }

    @Test
    void classifiesMathQueryWithSqrt() {
        Task task = taskWith(Map.of("userQuery", "Calculate the square root of 144"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("math", result.getOutputData().get("category"));
        assertEquals("calculator", result.getOutputData().get("toolName"));
        String parsed = (String) result.getOutputData().get("parsedExpression");
        assertNotNull(parsed);
        assertFalse(parsed.isBlank());
    }

    @Test
    void classifiesMathQueryWithArithmetic() {
        Task task = taskWith(Map.of("userQuery", "compute 5 + 3"));
        TaskResult result = worker.execute(task);

        assertEquals("math", result.getOutputData().get("category"));
        assertEquals("calculator", result.getOutputData().get("toolName"));
    }

    @Test
    void classifiesCodeQuery() {
        Task task = taskWith(Map.of("userQuery", "Write a function to sort a list"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("code", result.getOutputData().get("category"));
        assertEquals("interpreter", result.getOutputData().get("toolName"));
        assertEquals("Write a function to sort a list", result.getOutputData().get("codeRequest"));
    }

    @Test
    void classifiesCodeQueryWithJavaScript() {
        Task task = taskWith(Map.of("userQuery", "Write a javascript function for fibonacci"));
        TaskResult result = worker.execute(task);

        assertEquals("code", result.getOutputData().get("category"));
        assertEquals("javascript", result.getOutputData().get("language"));
    }

    @Test
    void classifiesCodeQueryDefaultsPython() {
        Task task = taskWith(Map.of("userQuery", "Implement a sorting algorithm"));
        TaskResult result = worker.execute(task);

        assertEquals("code", result.getOutputData().get("category"));
        assertEquals("python", result.getOutputData().get("language"));
    }

    @Test
    void classifiesSearchQueryAsDefault() {
        Task task = taskWith(Map.of("userQuery", "What is the capital of France?"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("search", result.getOutputData().get("category"));
        assertEquals("web_search", result.getOutputData().get("toolName"));
        assertEquals("What is the capital of France?", result.getOutputData().get("searchQuery"));
    }

    @Test
    void handlesNullUserQuery() {
        Map<String, Object> input = new HashMap<>();
        input.put("userQuery", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("category"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("category"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
