package graphqlapi.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GraphqlTaskWorkerTest {

    private final GraphqlTaskWorker worker = new GraphqlTaskWorker();

    @Test
    void taskDefName() {
        assertEquals("gql_task", worker.getTaskDefName());
    }

    @Test
    void deploysProjectInEnvironment() {
        Task task = taskWith(Map.of("project", "conductor-app", "env", "staging"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("conductor-app", result.getOutputData().get("project"));
        assertEquals("conductor-app-staging-deployed", result.getOutputData().get("result"));
    }

    @Test
    void handlesProductionEnvironment() {
        Task task = taskWith(Map.of("project", "my-service", "env", "production"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("my-service", result.getOutputData().get("project"));
        assertEquals("my-service-production-deployed", result.getOutputData().get("result"));
    }

    @Test
    void defaultsProjectWhenMissing() {
        Task task = taskWith(Map.of("env", "dev"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default-project", result.getOutputData().get("project"));
        assertEquals("default-project-dev-deployed", result.getOutputData().get("result"));
    }

    @Test
    void defaultsProjectWhenBlank() {
        Task task = taskWith(Map.of("project", "   ", "env", "staging"));
        TaskResult result = worker.execute(task);

        assertEquals("default-project", result.getOutputData().get("project"));
        assertEquals("default-project-staging-deployed", result.getOutputData().get("result"));
    }

    @Test
    void defaultsEnvWhenMissing() {
        Task task = taskWith(Map.of("project", "my-app"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("my-app", result.getOutputData().get("project"));
        assertEquals("my-app-dev-deployed", result.getOutputData().get("result"));
    }

    @Test
    void defaultsEnvWhenBlank() {
        Task task = taskWith(Map.of("project", "my-app", "env", "  "));
        TaskResult result = worker.execute(task);

        assertEquals("my-app", result.getOutputData().get("project"));
        assertEquals("my-app-dev-deployed", result.getOutputData().get("result"));
    }

    @Test
    void defaultsBothWhenEmpty() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default-project", result.getOutputData().get("project"));
        assertEquals("default-project-dev-deployed", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
