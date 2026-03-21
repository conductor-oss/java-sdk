package salesforceintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryLeadsWorkerTest {

    private final QueryLeadsWorker worker = new QueryLeadsWorker();

    @Test
    void taskDefName() {
        assertEquals("sfc_query_leads", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("query", "status = New"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("totalCount"));
        assertNotNull(result.getOutputData().get("leads"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
