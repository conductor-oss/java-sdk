package databaseintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConnectWorkerTest {

    private final ConnectWorker worker = new ConnectWorker();

    @Test
    void taskDefName() {
        assertEquals("dbi_connect", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("sourceDb", "postgres://src:5432/db", "targetDb", "postgres://tgt:5432/db"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("sourceConnectionId"));
        assertNotNull(result.getOutputData().get("targetConnectionId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
