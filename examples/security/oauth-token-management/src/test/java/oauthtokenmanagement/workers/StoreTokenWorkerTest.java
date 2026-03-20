package oauthtokenmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StoreTokenWorkerTest {

    private final StoreTokenWorker worker = new StoreTokenWorker();

    @Test void taskDefName() { assertEquals("otm_store_token", worker.getTaskDefName()); }

    @Test void storesTokenSuccessfully() {
        Task task = taskWith(Map.of("store_tokenData", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("store_token"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test void outputContainsStoreToken() {
        assertEquals(true, worker.execute(taskWith(Map.of())).getOutputData().get("store_token"));
    }

    @Test void outputContainsProcessed() {
        assertEquals(true, worker.execute(taskWith(Map.of())).getOutputData().get("processed"));
    }

    @Test void handlesMissingInputs() {
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(Map.of())).getStatus());
    }

    @Test void handlesExtraInputs() {
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(Map.of("extra", "val"))).getStatus());
    }

    @Test void deterministicOutput() {
        assertEquals(worker.execute(taskWith(Map.of())).getOutputData(),
                     worker.execute(taskWith(Map.of())).getOutputData());
    }

    @Test void statusIsCompleted() {
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(Map.of())).getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input)); return task;
    }
}
