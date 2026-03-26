package qualitygate.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeployWorkerTest {

    @Test
    void taskDefName() {
        DeployWorker worker = new DeployWorker();
        assertEquals("qg_deploy", worker.getTaskDefName());
    }

    @Test
    void returnsDeployedTrue() {
        DeployWorker worker = new DeployWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deployed"));
    }

    @Test
    void outputContainsDeployedKey() {
        DeployWorker worker = new DeployWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("deployed"));
    }

    @Test
    void alwaysCompletes() {
        DeployWorker worker = new DeployWorker();
        Task task = taskWith(Map.of("extra", "data"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deployed"));
    }

    @Test
    void isDeterministic() {
        DeployWorker worker = new DeployWorker();

        TaskResult result1 = worker.execute(taskWith(Map.of()));
        TaskResult result2 = worker.execute(taskWith(Map.of()));

        assertEquals(result1.getOutputData().get("deployed"), result2.getOutputData().get("deployed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
