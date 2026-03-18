package functioncalling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractFunctionCallWorkerTest {

    private final ExtractFunctionCallWorker worker = new ExtractFunctionCallWorker();

    @Test
    void taskDefName() {
        assertEquals("fc_extract_function_call", worker.getTaskDefName());
    }

    @Test
    void extractsFunctionNameFromLlmOutput() {
        Map<String, Object> llmOutput = Map.of(
                "reasoning", "Need to call get_stock_price",
                "functionCall", Map.of(
                        "name", "get_stock_price",
                        "arguments", Map.of("ticker", "AAPL")));
        Task task = taskWith(Map.of("llmOutput", llmOutput));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("get_stock_price", result.getOutputData().get("functionName"));
    }

    @Test
    void extractsArgumentsFromLlmOutput() {
        Map<String, Object> llmOutput = Map.of(
                "functionCall", Map.of(
                        "name", "get_stock_price",
                        "arguments", Map.of("ticker", "GOOG", "includeChange", false)));
        Task task = taskWith(Map.of("llmOutput", llmOutput));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) result.getOutputData().get("arguments");
        assertEquals("GOOG", args.get("ticker"));
        assertEquals(false, args.get("includeChange"));
    }

    @Test
    void setsValidatedToTrue() {
        Map<String, Object> llmOutput = Map.of(
                "functionCall", Map.of("name", "get_weather", "arguments", Map.of()));
        Task task = taskWith(Map.of("llmOutput", llmOutput));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("validated"));
    }

    @Test
    void handlesNullLlmOutput() {
        Map<String, Object> input = new HashMap<>();
        input.put("llmOutput", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("functionName"));
        assertEquals(false, result.getOutputData().get("validated"));
    }

    @Test
    void handlesMissingLlmOutput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("functionName"));
        assertEquals(false, result.getOutputData().get("validated"));
    }

    @Test
    void handlesLlmOutputWithoutFunctionCall() {
        Map<String, Object> llmOutput = Map.of("reasoning", "No function needed");
        Task task = taskWith(Map.of("llmOutput", llmOutput));
        TaskResult result = worker.execute(task);

        assertEquals("unknown", result.getOutputData().get("functionName"));
        assertEquals(false, result.getOutputData().get("validated"));
    }

    @Test
    void handlesNullFunctionName() {
        Map<String, Object> functionCall = new HashMap<>();
        functionCall.put("name", null);
        functionCall.put("arguments", Map.of("ticker", "MSFT"));
        Map<String, Object> llmOutput = Map.of("functionCall", functionCall);
        Task task = taskWith(Map.of("llmOutput", llmOutput));
        TaskResult result = worker.execute(task);

        assertEquals("unknown", result.getOutputData().get("functionName"));
        assertEquals(true, result.getOutputData().get("validated"));
    }

    @Test
    void handlesNullArguments() {
        Map<String, Object> functionCall = new HashMap<>();
        functionCall.put("name", "get_stock_price");
        functionCall.put("arguments", null);
        Map<String, Object> llmOutput = Map.of("functionCall", functionCall);
        Task task = taskWith(Map.of("llmOutput", llmOutput));
        TaskResult result = worker.execute(task);

        assertEquals("get_stock_price", result.getOutputData().get("functionName"));
        assertNotNull(result.getOutputData().get("arguments"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
