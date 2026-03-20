package crontrigger.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecordRunWorkerTest {

    private final RecordRunWorker worker = new RecordRunWorker();

    @Test
    void taskDefName() {
        assertEquals("cn_record_run", worker.getTaskDefName());
    }

    @Test
    void returnsRecordedTrue() {
        Task task = taskWith(Map.of("jobName", "daily-report", "result", "success"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
    }

    @Test
    void returnsFixedRunId() {
        Task task = taskWith(Map.of("jobName", "nightly-backup", "result", "success"));
        TaskResult result = worker.execute(task);

        assertEquals("run_fixed_001", result.getOutputData().get("runId"));
    }

    @Test
    void handlesFailureResult() {
        Task task = taskWith(Map.of("jobName", "health-check", "result", "failure"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
        assertEquals("run_fixed_001", result.getOutputData().get("runId"));
    }

    @Test
    void handlesNullJobName() {
        Map<String, Object> input = new HashMap<>();
        input.put("jobName", null);
        input.put("result", "success");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
    }

    @Test
    void handlesNullResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("jobName", "some-job");
        input.put("result", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
        assertEquals("run_fixed_001", result.getOutputData().get("runId"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("jobName", "report-gen", "result", "success"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("recorded"));
        assertTrue(result.getOutputData().containsKey("runId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
