package artifactmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuildWorkerTest {

    private final BuildWorker worker = new BuildWorker();

    @Test
    void taskDefName() {
        assertEquals("am_build", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("project", "auth-service", "version", "4.1.0"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsBuildId() {
        Task task = taskWith(Map.of("project", "auth-service", "version", "4.1.0"));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("buildId"));
    }

    @Test
    void returnsSuccess() {
        Task task = taskWith(Map.of("project", "auth-service", "version", "4.1.0"));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void handlesDefaultValues() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void buildIdIsDeterministic() {
        Task task = taskWith(Map.of("project", "api", "version", "1.0.0"));
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);
        assertEquals(r1.getOutputData().get("buildId"), r2.getOutputData().get("buildId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
