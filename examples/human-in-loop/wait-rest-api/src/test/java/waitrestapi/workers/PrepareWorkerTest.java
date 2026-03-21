package waitrestapi.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrepareWorkerTest {

    @Test
    void taskDefName() {
        PrepareWorker worker = new PrepareWorker();
        assertEquals("wapi_prepare", worker.getTaskDefName());
    }

    @Test
    void returnsReadyTrue() {
        PrepareWorker worker = new PrepareWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void outputIsDeterministic() {
        PrepareWorker worker = new PrepareWorker();

        Task task1 = taskWith(Map.of());
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of());
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("ready"), result2.getOutputData().get("ready"));
        assertEquals(result1.getStatus(), result2.getStatus());
    }

    @Test
    void completesWithAnyInput() {
        PrepareWorker worker = new PrepareWorker();
        Task task = taskWith(Map.of("extra", "data"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("ready"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
