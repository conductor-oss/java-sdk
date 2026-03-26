package automatedtesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SetupEnvTest {

    private final SetupEnv worker = new SetupEnv();

    @Test
    void taskDefName() {
        assertEquals("at_setup_env", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith("main");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsBuildId() {
        Task task = taskWith("main");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("buildId"));
    }

    @Test
    void outputContainsReady() {
        Task task = taskWith("main");
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void buildIdIsDeterministic() {
        Task task = taskWith("main");
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);
        assertEquals(r1.getOutputData().get("buildId"), r2.getOutputData().get("buildId"));
    }

    @Test
    void outputHasTwoFields() {
        Task task = taskWith("main");
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("buildId"));
        assertTrue(result.getOutputData().containsKey("ready"));
    }

    @Test
    void deterministicOutput() {
        Task task = taskWith("develop");
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);
        assertEquals(r1.getOutputData(), r2.getOutputData());
    }

    private Task taskWith(String branch) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("branch", branch);
        task.setInputData(input);
        return task;
    }
}
