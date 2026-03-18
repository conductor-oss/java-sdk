package runbookautomation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogOutcomeWorkerTest {

    private final LogOutcomeWorker worker = new LogOutcomeWorker();

    @Test
    void taskDefName() {
        assertEquals("ra_log_outcome", worker.getTaskDefName());
    }

    @Test
    void logsSuccessOutcome() {
        Task task = taskWith(Map.of("verified", true, "runbookId", "RB-TEST"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("outcome"));
    }

    @Test
    void logsFailureOutcome() {
        Task task = taskWith(Map.of("verified", false, "runbookId", "RB-TEST"));
        TaskResult result = worker.execute(task);

        assertEquals("failure", result.getOutputData().get("outcome"));
    }

    @Test
    void createsRealLogFile() {
        Task task = taskWith(Map.of("verified", true, "runbookId", "RB-LOG-TEST"));
        TaskResult result = worker.execute(task);

        String logFile = (String) result.getOutputData().get("logFile");
        assertNotNull(logFile);
        assertTrue(Files.exists(Path.of(logFile)), "Log file should exist on disk");
    }

    @Test
    void logFileContainsOutcome() throws Exception {
        Task task = taskWith(Map.of("verified", true, "runbookId", "RB-CONTENT-TEST"));
        TaskResult result = worker.execute(task);

        String logFile = (String) result.getOutputData().get("logFile");
        String content = Files.readString(Path.of(logFile));

        assertTrue(content.contains("success"));
        assertTrue(content.contains("RB-CONTENT-TEST"));
    }

    @Test
    void outputContainsLoggedAt() {
        Task task = taskWith(Map.of("verified", true));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("loggedAt"));
    }

    @Test
    void outputContainsDuration() {
        Task task = taskWith(Map.of("verified", true, "totalDurationMs", 1234L));
        TaskResult result = worker.execute(task);

        assertEquals(1234L, result.getOutputData().get("duration"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("outcome"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
