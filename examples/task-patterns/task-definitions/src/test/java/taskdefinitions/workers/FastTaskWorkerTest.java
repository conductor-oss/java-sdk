package taskdefinitions.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FastTaskWorkerTest {

    private final FastTaskWorker worker = new FastTaskWorker();

    @Test
    void taskDefName() {
        assertEquals("td_fast_task", worker.getTaskDefName());
    }

    @Test
    void executesSuccessfully() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("done"));
    }

    @Test
    void outputContainsDoneKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("done"));
        assertNotNull(result.getOutputData().get("done"));
    }

    @Test
    void doneValueIsTrue() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        Object done = result.getOutputData().get("done");
        assertInstanceOf(Boolean.class, done);
        assertTrue((Boolean) done);
    }

    @Test
    void statusIsCompleted() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesInputGracefully() {
        Task task = taskWith(Map.of("extraKey", "extraValue"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("done"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
