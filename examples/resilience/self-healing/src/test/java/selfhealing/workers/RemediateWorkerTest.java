package selfhealing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RemediateWorkerTest {

    @Test
    void taskDefName() {
        RemediateWorker worker = new RemediateWorker();
        assertEquals("sh_remediate", worker.getTaskDefName());
    }

    @Test
    void returnsFixedTrue() {
        RemediateWorker worker = new RemediateWorker();
        Task task = taskWith(Map.of(
                "diagnosis", "connection_pool_exhausted",
                "action", "restart_connection_pool"
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("fixed"));
    }

    @Test
    void handlesMissingInputs() {
        RemediateWorker worker = new RemediateWorker();
        Task task = taskWith(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("fixed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
