package apicalling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanApiCallWorkerTest {

    private final PlanApiCallWorker worker = new PlanApiCallWorker();

    @Test
    void taskDefName() {
        assertEquals("ap_plan_api_call", worker.getTaskDefName());
    }

    @Test
    void returnsSelectedApiAsGithub() {
        Task task = taskWith(Map.of(
                "userRequest", "Tell me about the Conductor repo",
                "apiCatalog", List.of(Map.of("name", "github"))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("github", result.getOutputData().get("selectedApi"));
    }

    @Test
    void returnsEndpoint() {
        Task task = taskWith(Map.of("userRequest", "Get repo info"));
        TaskResult result = worker.execute(task);

        String endpoint = (String) result.getOutputData().get("endpoint");
        assertNotNull(endpoint);
        assertTrue(endpoint.contains("api.github.com"));
    }

    @Test
    void returnsGetMethod() {
        Task task = taskWith(Map.of("userRequest", "Fetch data"));
        TaskResult result = worker.execute(task);

        assertEquals("GET", result.getOutputData().get("method"));
    }

    @Test
    void returnsParams() {
        Task task = taskWith(Map.of("userRequest", "Conductor repo details"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) result.getOutputData().get("params");
        assertNotNull(params);
        assertEquals("conductor-oss", params.get("owner"));
        assertEquals("conductor", params.get("repo"));
    }

    @Test
    void returnsAuthType() {
        Task task = taskWith(Map.of("userRequest", "Get info"));
        TaskResult result = worker.execute(task);

        assertEquals("bearer_token", result.getOutputData().get("authType"));
    }

    @Test
    void returnsResponseSchema() {
        Task task = taskWith(Map.of("userRequest", "Show repository"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) result.getOutputData().get("responseSchema");
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
        assertNotNull(schema.get("fields"));
    }

    @Test
    void handlesNullUserRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("userRequest", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("selectedApi"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("endpoint"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
