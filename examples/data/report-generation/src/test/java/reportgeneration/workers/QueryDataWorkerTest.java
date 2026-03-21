package reportgeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryDataWorkerTest {

    private final QueryDataWorker worker = new QueryDataWorker();

    @Test
    void taskDefName() {
        assertEquals("rg_query_data", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("reportType", "sales"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsData() {
        Task task = taskWith(Map.of("reportType", "sales"));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("data"));
    }

    @Test
    void returnsRecordCount() {
        Task task = taskWith(Map.of("reportType", "sales"));
        TaskResult result = worker.execute(task);
        assertEquals(7, result.getOutputData().get("recordCount"));
    }

    @Test
    void handlesDefaultReportType() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
