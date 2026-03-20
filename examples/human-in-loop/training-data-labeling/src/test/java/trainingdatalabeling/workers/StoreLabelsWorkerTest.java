package trainingdatalabeling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StoreLabelsWorkerTest {

    @Test
    void taskDefName() {
        StoreLabelsWorker worker = new StoreLabelsWorker();
        assertEquals("tdl_store_labels", worker.getTaskDefName());
    }

    @Test
    void returnsStoredTrue() {
        StoreLabelsWorker worker = new StoreLabelsWorker();
        Task task = taskWith(Map.of(
                "labels1", List.of("cat", "dog"),
                "labels2", List.of("cat", "dog"),
                "agreementPct", 100.0
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("stored"));
    }

    @Test
    void completesWithEmptyInput() {
        StoreLabelsWorker worker = new StoreLabelsWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("stored"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
