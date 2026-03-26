package databaseintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WriteWorkerTest {

    private final WriteWorker worker = new WriteWorker();

    @Test
    void taskDefName() {
        assertEquals("dbi_write", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("connectionId", "conn-tgt-1", "transformedRows", List.of(Map.of("id", 1))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("writtenCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
