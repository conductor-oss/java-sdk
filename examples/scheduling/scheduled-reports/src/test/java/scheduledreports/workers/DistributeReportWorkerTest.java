package scheduledreports.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DistributeReportWorkerTest {

    private final DistributeReportWorker worker = new DistributeReportWorker();

    @Test
    void taskDefName() {
        assertEquals("sch_distribute_report", worker.getTaskDefName());
    }

    @Test
    void distributesSuccessfully() {
        Task task = taskWith(Map.of("reportUrl", "https://reports.example.com/test.pdf",
                "recipients", "cfo@example.com", "reportType", "weekly-sales"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("delivered"));
    }

    @Test
    void returnsRecipientCount() {
        Task task = taskWith(Map.of("reportType", "weekly-sales"));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("recipientCount"));
    }

    @Test
    void returnsMethodEmail() {
        Task task = taskWith(Map.of("reportType", "weekly-sales"));
        TaskResult result = worker.execute(task);

        assertEquals("email", result.getOutputData().get("method"));
    }

    @Test
    void returnsSentAt() {
        Task task = taskWith(Map.of("reportType", "weekly-sales"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-08T06:00:00Z", result.getOutputData().get("sentAt"));
    }

    @Test
    void handlesNullReportType() {
        Map<String, Object> input = new HashMap<>();
        input.put("reportType", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("delivered"));
    }

    @Test
    void deliveredIsAlwaysTrue() {
        Task task = taskWith(Map.of("reportType", "monthly"));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("delivered"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
