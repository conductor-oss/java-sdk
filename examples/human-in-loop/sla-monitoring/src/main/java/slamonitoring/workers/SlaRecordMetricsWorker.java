package slamonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for sla_record_metrics task -- calculates wait duration and checks SLA compliance.
 *
 * Inputs:
 *   - waitStartTime: epoch millis when the WAIT task started
 *   - waitEndTime:   epoch millis when the WAIT task completed
 *   - slaMs:         SLA threshold in milliseconds
 *
 * Outputs:
 *   - waitDurationMs: actual wait duration in milliseconds
 *   - slaMet:         true if waitDurationMs <= slaMs
 *   - slaMs:          the SLA threshold that was used
 */
public class SlaRecordMetricsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sla_record_metrics";
    }

    @Override
    public TaskResult execute(Task task) {
        long waitStartTime = toLong(task.getInputData().get("waitStartTime"));
        long waitEndTime = toLong(task.getInputData().get("waitEndTime"));
        long slaMs = toLong(task.getInputData().get("slaMs"));

        long waitDurationMs = waitEndTime - waitStartTime;
        boolean slaMet = waitDurationMs <= slaMs;

        System.out.println("  [sla_record_metrics] Wait duration: " + waitDurationMs + "ms, SLA: " + slaMs + "ms, Met: " + slaMet);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("waitDurationMs", waitDurationMs);
        result.getOutputData().put("slaMet", slaMet);
        result.getOutputData().put("slaMs", slaMs);

        return result;
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}
