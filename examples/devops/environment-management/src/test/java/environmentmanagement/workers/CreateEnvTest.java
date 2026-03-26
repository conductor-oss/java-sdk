package environmentmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CreateEnvTest {

    private final CreateEnv worker = new CreateEnv();

    @Test
    void taskDefName() {
        assertEquals("em_create_env", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith("staging-pr-42", "production-like");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsEnvId() {
        Task task = taskWith("staging-pr-42", "production-like");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("envId"));
    }

    @Test
    void outputContainsCreated() {
        Task task = taskWith("staging-pr-42", "production-like");
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("created"));
    }

    @Test
    void envIdIsDeterministic() {
        Task task = taskWith("staging-pr-42", "production-like");
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);
        assertEquals(r1.getOutputData().get("envId"), r2.getOutputData().get("envId"));
    }

    @Test
    void outputHasTwoFields() {
        Task task = taskWith("staging-pr-42", "production-like");
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("envId"));
        assertTrue(result.getOutputData().containsKey("created"));
    }

    @Test
    void deterministicOutput() {
        Task task = taskWith("staging-pr-42", "production-like");
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);
        assertEquals(r1.getOutputData(), r2.getOutputData());
    }

    private Task taskWith(String envName, String template) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("envName", envName);
        input.put("template", template);
        task.setInputData(input);
        return task;
    }
}
