package databaseintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryWorkerTest {

    private final QueryWorker worker = new QueryWorker();

    @Test
    void taskDefName() {
        assertEquals("dbi_query", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("connectionId", "conn-src-1", "query", "SELECT * FROM users"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("rowCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
