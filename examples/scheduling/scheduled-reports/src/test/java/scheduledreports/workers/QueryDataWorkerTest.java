package scheduledreports.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryDataWorkerTest {

    private final QueryDataWorker worker = new QueryDataWorker();

    @Test
    void taskDefName() {
        assertEquals("sch_query_data", worker.getTaskDefName());
    }

    @Test
    void queriesDataSuccessfully() {
        Task task = taskWith(Map.of("reportType", "weekly-sales", "dateRange", "2026-03-01/2026-03-07"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3200, result.getOutputData().get("rowCount"));
    }

    @Test
    void returnsDataMap() {
        Task task = taskWith(Map.of("reportType", "weekly-sales", "dateRange", "2026-03-01/2026-03-07"));
        TaskResult result = worker.execute(task);

        assertInstanceOf(Map.class, result.getOutputData().get("data"));
    }

    @Test
    void returnsQueryTimeMs() {
        Task task = taskWith(Map.of("reportType", "monthly", "dateRange", "2026-02"));
        TaskResult result = worker.execute(task);

        assertEquals(850, result.getOutputData().get("queryTimeMs"));
    }

    @Test
    void handlesNullReportType() {
        Map<String, Object> input = new HashMap<>();
        input.put("reportType", null);
        input.put("dateRange", "2026-03-01/2026-03-07");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullDateRange() {
        Map<String, Object> input = new HashMap<>();
        input.put("reportType", "weekly-sales");
        input.put("dateRange", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("rowCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
