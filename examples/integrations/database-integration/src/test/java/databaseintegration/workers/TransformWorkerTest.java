package databaseintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformWorkerTest {

    private final TransformWorker worker = new TransformWorker();

    @Test
    void taskDefName() {
        assertEquals("dbi_transform", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("rows", List.of(Map.of("id", 1, "name", "Alice")), "rules", List.of("uppercase")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("transformedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
