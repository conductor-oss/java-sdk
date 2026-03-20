package slamonitoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SlaRecordMetricsWorkerTest {

    @Test
    void taskDefName() {
        SlaRecordMetricsWorker worker = new SlaRecordMetricsWorker();
        assertEquals("sla_record_metrics", worker.getTaskDefName());
    }

    @Test
    void slaMetWhenWithinThreshold() {
        SlaRecordMetricsWorker worker = new SlaRecordMetricsWorker();
        Task task = taskWith(Map.of(
                "waitStartTime", 1000L,
                "waitEndTime", 3000L,
                "slaMs", 5000L
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2000L, result.getOutputData().get("waitDurationMs"));
        assertEquals(true, result.getOutputData().get("slaMet"));
        assertEquals(5000L, result.getOutputData().get("slaMs"));
    }

    @Test
    void slaNotMetWhenExceedsThreshold() {
        SlaRecordMetricsWorker worker = new SlaRecordMetricsWorker();
        Task task = taskWith(Map.of(
                "waitStartTime", 1000L,
                "waitEndTime", 3000L,
                "slaMs", 1000L
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2000L, result.getOutputData().get("waitDurationMs"));
        assertEquals(false, result.getOutputData().get("slaMet"));
        assertEquals(1000L, result.getOutputData().get("slaMs"));
    }

    @Test
    void slaMetWhenExactlyAtThreshold() {
        SlaRecordMetricsWorker worker = new SlaRecordMetricsWorker();
        Task task = taskWith(Map.of(
                "waitStartTime", 1000L,
                "waitEndTime", 3000L,
                "slaMs", 2000L
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2000L, result.getOutputData().get("waitDurationMs"));
        assertEquals(true, result.getOutputData().get("slaMet"));
        assertEquals(2000L, result.getOutputData().get("slaMs"));
    }

    @Test
    void calculatesCorrectDuration() {
        SlaRecordMetricsWorker worker = new SlaRecordMetricsWorker();
        Task task = taskWith(Map.of(
                "waitStartTime", 1000L,
                "waitEndTime", 3000L,
                "slaMs", 10000L
        ));

        TaskResult result = worker.execute(task);

        assertEquals(2000L, result.getOutputData().get("waitDurationMs"));
    }

    @Test
    void handlesIntegerInputs() {
        SlaRecordMetricsWorker worker = new SlaRecordMetricsWorker();
        Task task = taskWith(Map.of(
                "waitStartTime", 1000,
                "waitEndTime", 3000,
                "slaMs", 5000
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2000L, result.getOutputData().get("waitDurationMs"));
        assertEquals(true, result.getOutputData().get("slaMet"));
    }

    @Test
    void handlesZeroDuration() {
        SlaRecordMetricsWorker worker = new SlaRecordMetricsWorker();
        Task task = taskWith(Map.of(
                "waitStartTime", 5000L,
                "waitEndTime", 5000L,
                "slaMs", 1000L
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0L, result.getOutputData().get("waitDurationMs"));
        assertEquals(true, result.getOutputData().get("slaMet"));
    }

    @Test
    void handlesMissingInputsGracefully() {
        SlaRecordMetricsWorker worker = new SlaRecordMetricsWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0L, result.getOutputData().get("waitDurationMs"));
        assertEquals(true, result.getOutputData().get("slaMet"));
        assertEquals(0L, result.getOutputData().get("slaMs"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        SlaRecordMetricsWorker worker = new SlaRecordMetricsWorker();
        Task task = taskWith(Map.of(
                "waitStartTime", 1000L,
                "waitEndTime", 3000L,
                "slaMs", 5000L
        ));

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("waitDurationMs"));
        assertTrue(result.getOutputData().containsKey("slaMet"));
        assertTrue(result.getOutputData().containsKey("slaMs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
