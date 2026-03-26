package llmretry.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RetryReportWorkerTest {

    private final RetryReportWorker worker = new RetryReportWorker();

    @Test
    void taskDefName() {
        assertEquals("retry_report", worker.getTaskDefName());
    }

    @Test
    void returnsSummaryOnCompleted() {
        Task task = taskWith(Map.of(
                "response", "Conductor handles retries so your AI pipeline never drops requests.",
                "attempts", 3
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Retry succeeded", result.getOutputData().get("summary"));
    }

    @Test
    void handlesStringAttempts() {
        Task task = taskWith(Map.of(
                "response", "some response",
                "attempts", "5"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Retry succeeded", result.getOutputData().get("summary"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
