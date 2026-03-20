package approvaldelegation.workers;

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
        assertEquals("ad_finalize", worker.getTaskDefName());
    }

    @Test
    void returnsDoneTrue() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("done"));
    }

    @Test
    void completesWithAnyInput() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(Map.of("approvedBy", "delegate@example.com"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("done"));
    }

    @Test
    void outputContainsDoneKey() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("done"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
