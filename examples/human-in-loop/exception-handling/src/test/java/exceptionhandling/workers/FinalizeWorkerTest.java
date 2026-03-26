package exceptionhandling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalizeWorkerTest {

    @Test
    void taskDefName() {
        FinalizeWorker worker = new FinalizeWorker();
        assertEquals("eh_finalize", worker.getTaskDefName());
    }

    @Test
    void returnsFinalizedTrue() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("finalized"));
    }

    @Test
    void alwaysCompletes() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(Map.of("extra", "data"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("finalized"));
    }

    @Test
    void outputContainsFinalizedKey() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("finalized"));
        assertEquals(true, result.getOutputData().get("finalized"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
