package apigatewayrouting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformResponseWorkerTest {

    private final TransformResponseWorker worker = new TransformResponseWorker();

    @Test
    void taskDefName() {
        assertEquals("gw_transform_response", worker.getTaskDefName());
    }

    @Test
    void transformsResponse() {
        Map<String, Object> response = Map.of("orderId", "ORD-1");
        Task task = taskWith(Map.of("serviceResponse", response, "clientVersion", "v2"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(response, result.getOutputData().get("transformedBody"));
    }

    @Test
    void passesThoughServiceResponse() {
        Map<String, Object> response = Map.of("data", "test");
        Task task = taskWith(Map.of("serviceResponse", response, "clientVersion", "v1"));
        TaskResult result = worker.execute(task);

        assertEquals(response, result.getOutputData().get("transformedBody"));
    }

    @Test
    void handlesNullServiceResponse() {
        Map<String, Object> input = new HashMap<>();
        input.put("serviceResponse", null);
        input.put("clientVersion", "v2");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNull(result.getOutputData().get("transformedBody"));
    }

    @Test
    void handlesNullClientVersion() {
        Map<String, Object> input = new HashMap<>();
        input.put("serviceResponse", Map.of("a", "b"));
        input.put("clientVersion", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsTransformedBody() {
        Task task = taskWith(Map.of("serviceResponse", "text", "clientVersion", "v1"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("transformedBody"));
    }

    @Test
    void preservesNestedData() {
        Map<String, Object> nested = Map.of("inner", Map.of("key", "value"));
        Task task = taskWith(Map.of("serviceResponse", nested, "clientVersion", "v2"));
        TaskResult result = worker.execute(task);

        assertEquals(nested, result.getOutputData().get("transformedBody"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
